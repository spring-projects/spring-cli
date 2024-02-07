/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.cli.merger.ai;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependency;
import org.springframework.cli.runtime.engine.actions.handlers.InjectMavenActionHandler;
import org.springframework.cli.util.ClassNameExtractor;
import org.springframework.cli.util.MavenDependencyReader;
import org.springframework.cli.util.PomReader;
import org.springframework.cli.util.TerminalMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.cli.util.PropertyFileUtils.mergeProperties;

public class ProjectArtifactProcessor {
	private final List<ProjectArtifact> projectArtifacts;

	private final Path projectPath;

	private final TerminalMessage terminalMessage;

	private final Pattern compiledGroupIdPattern;

	private final Pattern compiledArtifactIdPattern;

	public ProjectArtifactProcessor(List<ProjectArtifact> projectArtifacts, Path projectPath, TerminalMessage terminalMessage) {
		this.projectArtifacts = projectArtifacts;
		this.projectPath = projectPath;
		this.terminalMessage = terminalMessage;
		compiledGroupIdPattern = Pattern.compile("<groupId>(.*?)</groupId>");
		compiledArtifactIdPattern = Pattern.compile("<artifactId>(.*?)</artifactId>");
	}

	public ProcessArtifactResult process() {
		return processArtifacts(projectArtifacts, projectPath, terminalMessage);
	}

	private ProcessArtifactResult<Void> processArtifacts(List<ProjectArtifact> projectArtifacts, Path projectPath, TerminalMessage terminalMessage) {
		ProcessArtifactResult<Void> processArtifactResult = new ProcessArtifactResult<>();
		for (ProjectArtifact projectArtifact : projectArtifacts) {
			try {
				ProjectArtifactType artifactType = projectArtifact.getArtifactType();
				switch (artifactType) {
					case SOURCE_CODE:
						writeSourceCode(projectArtifact, projectPath);
						break;
					case TEST_CODE:
						writeTestCode(projectArtifact, projectPath);
						break;
					case MAVEN_DEPENDENCIES:
						writeMavenDependencies(projectArtifact, projectPath, terminalMessage);
						break;
					case APPLICATION_PROPERTIES:
						writeApplicationProperties(projectArtifact, projectPath);
						break;
					case MAIN_CLASS:
						updateMainApplicationClassAnnotations(projectArtifact, projectPath, terminalMessage);
						break;
					case HTML:
						writeHtml(projectArtifact, projectPath);
						break;
					default:
						processArtifactResult.addToNotProcessed(projectArtifact);
						break;
				}
			}
			catch (IOException ex) {
				ex.printStackTrace();
				throw new SpringCliException("Could not write project artifact.", ex);
			}
		}
		return processArtifactResult;
	}

	private void writeSourceCode(ProjectArtifact projectArtifact, Path projectPath) throws IOException {
		String packageName = this.calculatePackageForArtifact(projectArtifact);
		ClassNameExtractor classNameExtractor = new ClassNameExtractor();
		Optional<String> className = classNameExtractor.extractClassName(projectArtifact.getText());
		if (className.isPresent()) {
			Path output = createSourceFile(projectPath, packageName, className.get() + ".java");
			try (Writer writer = new BufferedWriter(new FileWriter(output.toFile()))) {
				writer.write(projectArtifact.getText());
			}
		}
	}

	private void writeTestCode(ProjectArtifact projectArtifact, Path projectPath) throws IOException {
		// TODO parameterize better to reduce code duplication
		String packageName = this.calculatePackageForArtifact(projectArtifact);
		ClassNameExtractor classNameExtractor = new ClassNameExtractor();
		Optional<String> className = classNameExtractor.extractClassName(projectArtifact.getText());
		if (className.isPresent()) {
			Path output = createTestFile(projectPath, packageName, className.get() + ".java");
			try (Writer writer = new BufferedWriter(new FileWriter(output.toFile()))) {
				writer.write(projectArtifact.getText());
			}
		}
	}

	private void writeMavenDependencies(ProjectArtifact projectArtifact, Path projectPath, TerminalMessage terminalMessage) {
		PomReader pomReader = new PomReader();
		Path currentProjectPomPath = this.projectPath.resolve("pom.xml");
		if (Files.notExists(currentProjectPomPath)) {
			throw new SpringCliException("Could not find pom.xml in " + this.projectPath + ".  Make sure you are running the command in the project's root directory.");
		}
		Model currentModel = pomReader.readPom(currentProjectPomPath.toFile());
		List<Dependency> currentDependencies = currentModel.getDependencies();


		MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
		//projectArtifact.getText() contains a list of <dependency> elements
		String[] mavenDependencies = mavenDependencyReader.parseMavenSection(projectArtifact.getText());

		InjectMavenActionHandler injectMavenActionHandler = new InjectMavenActionHandler(null, new HashMap<>(), projectPath, terminalMessage);

		for (String candidateDependencyText : mavenDependencies) {
			if (!candidateDependencyAlreadyPresent(getProjectDependency(candidateDependencyText), currentDependencies)) {
				injectMavenActionHandler.injectDependency(new InjectMavenDependency(candidateDependencyText));
			}
		}

		try {
			injectMavenActionHandler.exec();
		}
		catch (Exception ex) {
			terminalMessage.print("Could not inject Maven dependencies.  Look at pom.xml contents for messages on what went wrong, e.g. 'No version provided'\n" + ex.getMessage());
		}
	}

	private boolean candidateDependencyAlreadyPresent(ProjectDependency toMergeDependency, List<Dependency> currentDependencies) {
		String candidateGroupId = toMergeDependency.getGroupId();
		String candidateArtifactId = toMergeDependency.getArtifactId();
		boolean candidateDependencyAlreadyPresent = false;
		for (Dependency currentDependency : currentDependencies) {
			String currentGroupId = currentDependency.getGroupId();
			String currentArtifactId = currentDependency.getArtifactId();
			if (candidateGroupId.equals(currentGroupId) && candidateArtifactId.equals(currentArtifactId)) {
				candidateDependencyAlreadyPresent = true;
				break;
			}
		}
		return candidateDependencyAlreadyPresent;

	}

	private String massageText(String text) {
		if (text.contains("<dependencies>")) {
			return text;
		} else {
			return "<dependencies>" + text + "</dependencies>";
		}
	}

	private ProjectDependency getProjectDependency(String xml) {
		String groupId = null;
		String artifactId = null;
		try {
			groupId = extractValue(xml, this.compiledGroupIdPattern);
			artifactId = extractValue(xml, this.compiledArtifactIdPattern);

		} catch (Exception ex) {
			throw new SpringCliException("Exception processing dependency: " + xml, ex);
		}
		if (groupId == null || artifactId == null) {
			throw new SpringCliException("Could not process dependency: " + xml);
		}
		return new ProjectDependency(groupId, artifactId);
	}

	private static String extractValue(String xml, Pattern compiledPattern) {
		Matcher matcher = compiledPattern.matcher(xml);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	private void writeApplicationProperties(ProjectArtifact projectArtifact, Path projectPath) throws IOException {
		Path applicationPropertiesPath =  projectPath.resolve("src").resolve("main").resolve("resources").resolve("application.properties");

		if (Files.notExists(applicationPropertiesPath)) {
			createFile(applicationPropertiesPath);
		}
		Properties srcProperties = new Properties();
		Properties destProperties = new Properties();
		srcProperties.load(IOUtils.toInputStream(projectArtifact.getText(), StandardCharsets.UTF_8));
		destProperties.load(new FileInputStream(applicationPropertiesPath.toFile()));
		Properties mergedProperties = mergeProperties(srcProperties, destProperties);
		mergedProperties.store(new FileWriter(applicationPropertiesPath.toFile()), "updated by spring ai add");
	}

	private void updateMainApplicationClassAnnotations(ProjectArtifact projectArtifact, Path projectPath, TerminalMessage terminalMessage) {
		// TODO mer
	}

	private void writeHtml(ProjectArtifact projectArtifact, Path projectPath) throws IOException {
		String html = projectArtifact.getText();
		String fileName = extractFilenameFromComment(html);
		if (fileName != null) {
			Path htmlFile = projectPath.resolve(fileName);
			createFile(htmlFile);
			try (Writer writer = new BufferedWriter(new FileWriter(htmlFile.toFile()))) {
				writer.write(projectArtifact.getText());
			}
		}
	}


	private String calculatePackageForArtifact(ProjectArtifact projectArtifact) {
		String packageToUse = "com.example.ai";
		try (BufferedReader reader = new BufferedReader(new StringReader(projectArtifact.getText()))) {
			String firstLine = reader.readLine();
			if (firstLine.contains("package")) {
				String regex = "^package\\s+([a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*);";
				Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
				Matcher matcher = pattern.matcher(firstLine);
				// Find the package statement and extract the package name
				String packageName = "";
				if (matcher.find()) {
					packageToUse = matcher.group(1);
				}
			}
		} catch (IOException e) {
			throw new SpringCliException("Could not parse package name from Project Artifact: " +
					projectArtifact.getText());
		}
		return packageToUse;
	}

	private static String extractFilenameFromComment(String content) {
		String commentPattern = "<!--\\s*filename:\\s*(\\S+\\.html)\\s*-->";
		Pattern pattern = Pattern.compile(commentPattern);
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}


	private Path createSourceFile(Path projectPath, String packageName, String fileName) throws IOException {
		Path sourceFile = resolveSourceFile(projectPath, packageName, fileName);
		createFile(sourceFile);
		return sourceFile;
	}

	private Path createTestFile(Path projectPath, String packageName, String fileName) throws IOException {
		Path sourceFile = resolveTestFile(projectPath, packageName, fileName);
		createFile(sourceFile);
		return sourceFile;
	}

	public Path resolveSourceFile(Path projectPath, String packageName, String fileName) {
		Path sourceDirectory = projectPath.resolve("src").resolve("main").resolve("java");
		return resolvePackage(sourceDirectory, packageName).resolve(fileName);
	}

	public Path resolveTestFile(Path projectPath, String packageName, String fileName) {
		Path sourceDirectory = projectPath.resolve("src").resolve("test").resolve("java");
		return resolvePackage(sourceDirectory, packageName).resolve(fileName);
	}

	private static Path resolvePackage(Path directory, String packageName) {
		return directory.resolve(packageName.replace('.', '/'));
	}

	private void createFile(Path file) throws IOException {
		if (Files.exists(file)) {
			//System.out.println("deleting file " + file.toAbsolutePath());
			Files.delete(file);
		}
		Files.createDirectories(file.getParent());
		if (Files.notExists(file)) {
			Files.createFile(file);
		}
	}
}

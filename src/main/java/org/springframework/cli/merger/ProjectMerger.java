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


package org.springframework.cli.merger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.Java17Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.ChangePropertyValue;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.xml.tree.Xml.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor.ResolutionMethod;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.util.PomReader;
import org.springframework.cli.util.RootPackageFinder;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.FileSystemResource;

import static org.springframework.cli.util.RefactorUtils.refactorPackage;

/**
 * Performs the refactoring steps to merge two Spring projects
 *
 * @author Mark Pollack
 */
public class ProjectMerger {

	private static final Logger logger = LoggerFactory.getLogger(ProjectMerger.class);

	private Path toMergeProjectPath;

	private Path currentProjectPath;

	private String projectName;

	private final TerminalMessage terminalMessage;

	/**
	 * Create a new instance
	 *
	 * @param toMergeProjectPath The Path where the new project to merge is located
	 * @param currentProjectPath The Path where the current project is located
	 * @param projectName used to change the name of README files
	 * @param terminalMessage
	 */
	public ProjectMerger(Path toMergeProjectPath, Path currentProjectPath, String projectName, TerminalMessage terminalMessage) {
		this.toMergeProjectPath = toMergeProjectPath;
		this.currentProjectPath = currentProjectPath;
		this.projectName = projectName;
		this.terminalMessage = terminalMessage;
	}

	public void merge() {
		PomReader pomReader = new PomReader();
		Path toMergeProjectPomPath = this.toMergeProjectPath.resolve("pom.xml");
		if (toMergeProjectPomPath == null) {
			throw new SpringCliException("Could not find pom.xml in " + this.toMergeProjectPath);
		}
		Path currentProjectPomPath = this.currentProjectPath.resolve("pom.xml");
		if (currentProjectPomPath == null) {
			throw new SpringCliException("Could not find pom.xml in " + this.currentProjectPath);
		}
		Model currentModel = pomReader.readPom(currentProjectPomPath.toFile());
		Model toMergeModel = pomReader.readPom(toMergeProjectPomPath.toFile());

		List<Path> paths = new ArrayList<>();
		paths.add(currentProjectPomPath);
		MavenParser mavenParser = MavenParser.builder().build();

		MergerPreCheck mergerPreCheck = new MergerPreCheck();
		mergerPreCheck.canMergeProject(currentModel, toMergeModel, this.toMergeProjectPath);

		try {
			// Maven merges
			mergeMavenProperties(currentProjectPomPath, toMergeModel);
			mergeMavenDependencyManagement(currentProjectPomPath, toMergeModel, paths, mavenParser);
			mergeMavenDependencies(currentProjectPomPath, currentModel, toMergeModel, paths, mavenParser);

			// Code Refactoring
			refactorToMergeCodebase();
			copyToMergeCodebase();
			// Copy and merge files

			mergeSpringBootApplicationClassAnnotations();
		} catch (IOException ex) {
			throw new SpringCliException("Error merging projects.", ex);
		}
	}





	private void mergeSpringBootApplicationClassAnnotations() throws IOException {

		logger.debug("Looking for @SpringBootApplication in directory " + this.toMergeProjectPath.toFile());
		Optional<File> springBootApplicationFile = RootPackageFinder.findSpringBootApplicationFile(this.toMergeProjectPath.toFile());

		if (springBootApplicationFile.isPresent()) {
			CollectAnnotationAndImportInformationRecipe collectAnnotationAndImportInformationRecipe = new CollectAnnotationAndImportInformationRecipe();
			Consumer<Throwable> onError = e -> {
				logger.error("error in javaParser execution", e);
			};
			InMemoryExecutionContext executionContext = new InMemoryExecutionContext(onError);
			List<Path> paths = new ArrayList<>();
			paths.add(springBootApplicationFile.get().toPath());
			JavaParser javaParser = new Java17Parser.Builder().build();
			List<? extends SourceFile> compilationUnits = javaParser.parse(paths, null, executionContext);
			collectAnnotationAndImportInformationRecipe.run(compilationUnits);

			List<Annotation> declaredAnnotations = collectAnnotationAndImportInformationRecipe.getDeclaredAnnotations();
			List<String> declaredImports = collectAnnotationAndImportInformationRecipe.getDeclaredImports();


			Map<String, String> annotationImportMap = new HashMap<>();
			for (Annotation declaredAnnotation : declaredAnnotations) {
				if (declaredAnnotation.toString().startsWith("@SpringBootApplication")) {
					continue;
				}
				for (String declaredImport : declaredImports) {
					//get the import statement that matches the annotation
					if (declaredImport.contains(declaredAnnotation.getSimpleName())) {
						annotationImportMap.put(declaredAnnotation.toString(), declaredImport);
					}
				}
			}

			logger.debug("Looking for @SpringBootApplication in directory " + this.currentProjectPath.toFile());
			Optional<File> currentSpringBootApplicationFile = RootPackageFinder.findSpringBootApplicationFile(this.currentProjectPath.toFile());
			if (currentSpringBootApplicationFile.isPresent()) {
				executionContext = new InMemoryExecutionContext(onError);
				paths = new ArrayList<>();
				paths.add(currentSpringBootApplicationFile.get().toPath());
				javaParser = new Java17Parser.Builder().build();
				compilationUnits = javaParser.parse(paths, null, executionContext);
				for (Entry<String, String> annotationImportEntry : annotationImportMap.entrySet()) {
					String annotation = annotationImportEntry.getKey();
					String importStatement = annotationImportEntry.getValue();
					AddImport addImport = new AddImport(importStatement, null, false);
					AddImportRecipe addImportRecipe = new AddImportRecipe(addImport);
					List<Result> results = addImportRecipe.run(compilationUnits).getResults();
					updateSpringApplicationClass(currentSpringBootApplicationFile.get().toPath(), results);

					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging Main Spring Boot Application class annotation: " + annotation);
					this.terminalMessage.shellPrint(sb.toAttributedString());

					injectAnnotation(currentSpringBootApplicationFile.get().toPath() ,annotation);
					//AddAnnotationToClassRecipe addAnnotationToClassRecipe = new AddAnnotationToClassRecipe(annotation);
					//results = addAnnotationToClassRecipe.run(compilationUnits);
					//updateSpringApplicationClass(currentSpringBootApplicationFile.get().toPath(), results);
				}
			}
		}



	}

	private void injectAnnotation(Path pathToFile, String annotation) {
		try {
			List<String> lines = Files.readAllLines(pathToFile);
			int injectIndex = indexFromMarkerString("@SpringBootApplication", lines);
			if (injectIndex != -1) {
				lines.add(injectIndex + 1, annotation);
				Files.write(pathToFile, lines, Charset.defaultCharset());
			} else {
				logger.debug("Did not add annotation" + annotation + " to file " + pathToFile);
			}
		} catch (IOException ex) {
			throw new SpringCliException("Could not add annotation " + annotation + " to file " + pathToFile, ex);
		}
	}

	/**
	 * @param marker the string we are looking to insert before or after
	 * @param lines the lines of text
	 * @return the index to insert a new line, -1 if no match found.
	 */
	private int indexFromMarkerString(String marker, List<String> lines) {
		int i = 0;
		for (Iterator<String> it = lines.iterator(); it.hasNext(); i++) {
			String line = it.next();
			if (line.contains(marker)) {
				return i;
			}
		}
		if (i == lines.size()) {
			return -1;
		}
		else {
			return i;
		}
	}

	private void copyToMergeCodebase() throws IOException {
		File fromDir = this.toMergeProjectPath.toFile();
		File toDir = this.currentProjectPath.toFile();
		DirectoryScanner ds = new DirectoryScanner();
		ds.setBasedir(fromDir);
		// TODO /** prob doesn't work on windows
		ds.setExcludes(new String[]{ ".mvn/**", ".idea"});
		ds.scan();
		String[] fileNames = ds.getIncludedFiles();
		Optional<File> springBootApplicationFile = RootPackageFinder.findSpringBootApplicationFile(this.toMergeProjectPath.toFile());
		for (String fileName : fileNames) {
			File srcFile = new File(fromDir, fileName);
			File destFile = new File(toDir, fileName);
			if (srcFile.getName().equals("pom.xml")  || srcFile.getName().equals("LICENSE")) {
				continue;
			}
			// hack to avoid bringing over any gradle files for now as this POC is maven only.
			if (srcFile.getName().contains("gradle")) {
				continue;
			}
			// Change readme file name have the project name that is being merged into the code base
			if (FilenameUtils.getBaseName(srcFile.getName()).equalsIgnoreCase("README")) {
				Path path = Paths.get(FilenameUtils.getPath(destFile.getName()),
						FilenameUtils.getBaseName(destFile.getName())
								+ "-" + projectName +
								"." + FilenameUtils.getExtension(destFile.getName()));
				destFile = path.toFile();
			}
			if (springBootApplicationFile.isPresent()) {
				if (srcFile.equals(springBootApplicationFile.get())) {
					continue;
				}
			}
			if (destFile.exists()) {
				Optional<String> extension = getExtension(srcFile.getName());
				if (extension.isPresent() && extension.get().equals("properties")) {
					mergeAndWriteProperties(srcFile, destFile);
				} else if (extension.isPresent() && (extension.get().equals("yaml") || extension.get().equals("yml")) ) {
					mergeAndWriteYaml(srcFile, destFile);
				} else {
					logger.debug("WARNING: Not copying file as it already exists: " + srcFile);
				}
				//TODO handle renaming readme.adoc etc.
			} else {
				logger.debug("Copying srcFile = " + srcFile + " to destFile = " + destFile);
				FileUtils.getFileUtils().copyFile(srcFile, destFile);
			}

		}
	}

	private void mergeAndWriteYaml(File srcFile, File destFile) throws FileNotFoundException {

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.WHITE));
		sb.append(System.lineSeparator());
		sb.append("Merging Spring Boot application.yaml file...");
		this.terminalMessage.shellPrint(sb.toAttributedString());


		YamlMapFactoryBean factory = new YamlMapFactoryBean();
		factory.setResolutionMethod(ResolutionMethod.OVERRIDE_AND_IGNORE);
		FileSystemResource srcFileResource = new FileSystemResource(srcFile);
		FileSystemResource destFileResource = new FileSystemResource(destFile);
		factory.setResources(srcFileResource, destFileResource);
		Map<String, Object> yamlAsMap = factory.getObject();
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		dumperOptions.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
		Yaml yaml = new Yaml(dumperOptions);
		destFile.delete();
		yaml.dump(yamlAsMap, new PrintWriter(destFile));
	}

	private void mergeAndWriteProperties(File srcFile, File destFile) throws IOException {


		Properties srcProperties = new Properties();
		Properties destProperties = new Properties();
		srcProperties.load(new FileInputStream(srcFile));
		destProperties.load(new FileInputStream(destFile));
		Properties mergedProperties = mergeProperties(srcProperties, destProperties);
		// look into handling a merge of maven-wrapper.properties - should only merge using latest versions.
		if (!mergedProperties.equals(srcProperties)) {

			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.WHITE));
			sb.append("Merging Spring Application property file...");
			this.terminalMessage.shellPrint(sb.toAttributedString());

			mergedProperties.store(new FileWriter(destFile), "updated by spring cli");
		}
	}

	public Optional<String> getExtension(String filename) {
		return Optional.ofNullable(filename)
				.filter(f -> f.contains("."))
				.map(f -> f.substring(filename.lastIndexOf(".") + 1));
	}

	private Properties mergeProperties(Properties... properties) {
		Properties mergedProperties = new Properties();
		for (Properties property : properties) {
			mergedProperties.putAll(property);
		}
		return mergedProperties;
	}

	private void refactorToMergeCodebase() {

		logger.debug("Looking for @SpringBootApplication in directory " + this.currentProjectPath.toFile());
		Optional<String> currentRootPackageName = RootPackageFinder.findRootPackage(this.currentProjectPath.toFile());
		if (currentRootPackageName.isEmpty()) {
			System.out.println("Could find root package containing class with @SpringBootApplication in " + this.currentProjectPath.toFile());
			System.out.println("Stopping");
			return;
		}

		logger.debug("Looking for @SpringBootApplication in directory " + this.toMergeProjectPath.toFile());
		Optional<String> toMergeRootPackageName = RootPackageFinder.findRootPackage(this.toMergeProjectPath.toFile());
		if (toMergeRootPackageName.isEmpty()) {
			System.out.println("Could find root package containing class with @SpringBootApplication in " + this.toMergeProjectPath.toFile());
			System.out.println("Stopping");
			return;
		}

		//System.out.println("Refactoring to merge code base.  From package " + toMergeRootPackageName.get() + " to " + currentRootPackageName.get());

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.WHITE));
		sb.append("Refactoring code base that is to be merged to package name " + currentRootPackageName.get());
		this.terminalMessage.shellPrint(sb.toAttributedString());
		refactorPackage(currentRootPackageName.get(), toMergeRootPackageName.get(), this.toMergeProjectPath);
		logger.debug("look in " + this.toMergeProjectPath + " to see if refactoring of 'to merge code base' was done correctly");
	}

	private void mergeMavenDependencies(Path currentProjectPomPath, Model currentModel, Model toMergeModel, List<Path> paths, MavenParser mavenParser) throws IOException {
		logger.debug("mergeMavenDependencies: Merging Maven Dependencies...");
		List<Dependency> toMergeModelDependencies = toMergeModel.getDependencies();
		List<Dependency> currentDependencies = currentModel.getDependencies();

		for (Dependency candidateDependency : toMergeModelDependencies) {
			if (candidateDependencyAlreadyPresent(candidateDependency, currentDependencies)) {
				logger.debug("mergeMavenDependencies: Not merging dependency " + candidateDependency);
			} else {
				List<Document> parsedPomFiles = mavenParser.parse(paths, this.currentProjectPath, getExecutionContext());
				String scope = candidateDependency.getScope();
				if (scope == null) {
					scope = "compile";
				}
				AddDependency addDependency = getRecipeAddDependency(candidateDependency.getGroupId(), candidateDependency.getArtifactId(), candidateDependency.getVersion(), scope, "org.springframework.boot.SpringApplication");

				List<Result> resultList = addDependency.run(parsedPomFiles).getResults();
				if (!resultList.isEmpty()) {
					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging dependency " + candidateDependency.getGroupId() + ":" + candidateDependency.getArtifactId());
					this.terminalMessage.shellPrint(sb.toAttributedString());
				}
				updatePomFile(currentProjectPomPath, resultList);
			}
		}
	}

	private boolean candidateDependencyAlreadyPresent(Dependency candidateDependency, List<Dependency> currentDependencies) {
		String candidateGroupId = candidateDependency.getGroupId();
		String candidateArtifactId = candidateDependency.getArtifactId();
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

	private void mergeMavenDependencyManagement(Path currentProjectPomPath, Model modelToMerge, List<Path> paths, MavenParser mavenParser) throws IOException {
		DependencyManagement dependencyManagement = modelToMerge.getDependencyManagement();
		if (dependencyManagement != null) {
			List<Dependency> dependencies = dependencyManagement.getDependencies();

			for (Dependency dependency : dependencies) {
				AddManagedDependency addManagedDependency = getRecipeAddManagedDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(),
						dependency.getType(), dependency.getClassifier());

				List<? extends SourceFile> pomFiles = mavenParser.parse(paths, this.currentProjectPath, getExecutionContext());
				List<Result> resultList = addManagedDependency.run(pomFiles).getResults();
				if (!resultList.isEmpty()) {
					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging dependency management section " + dependency.getGroupId() + ":" + dependency.getArtifactId());
					this.terminalMessage.shellPrint(sb.toAttributedString());
				}
				updatePomFile(currentProjectPomPath, resultList);
			}
		}
	}

	private void mergeMavenProperties(Path currentProjectPomPath, Model modelToMerge) throws IOException {
		List<Path> paths = new ArrayList<>();
		paths.add(currentProjectPomPath);
		MavenParser mavenParser = MavenParser.builder().build();

		Properties propertiesToMerge = modelToMerge.getProperties();
		Set<String> keysToMerge = propertiesToMerge.stringPropertyNames();

		for (String keyToMerge : keysToMerge) {
			// TODO may want to do something special in case java.version is set to be different
			ChangePropertyValue changePropertyValueRecipe = new ChangePropertyValue(keyToMerge, propertiesToMerge.getProperty(keyToMerge), true, true);
			List<? extends SourceFile> pomFiles = mavenParser.parse(paths, this.currentProjectPath, getExecutionContext());
			List<Result> resultList = changePropertyValueRecipe.run(pomFiles).getResults();
			if (!resultList.isEmpty()) {
				AttributedStringBuilder sb = new AttributedStringBuilder();
				sb.style(sb.style().foreground(AttributedStyle.WHITE));
				sb.append("Merging maven property key " + keyToMerge);
				this.terminalMessage.shellPrint(sb.toAttributedString());
			}
			updatePomFile(currentProjectPomPath, resultList);

		}
	}


	private void updateSpringApplicationClass(Path pathToCurrentSpringApplicationClass, List<Result> resultList) throws IOException {
		if (resultList.isEmpty()) {
			logger.debug("No update of SpringApplication class in " + pathToCurrentSpringApplicationClass);
		}

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.WHITE));
		sb.append("Adding import statements and annotations to @SpringApplication class");
		this.terminalMessage.shellPrint(sb.toAttributedString());

		for (Result result : resultList) {
			// write updated file.
			try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(pathToCurrentSpringApplicationClass)) {
				//System.out.println("Updating pom.xml in " + pathToCurrentSpringApplicationClass);
				sourceFileWriter.write(result.getAfter().printAllTrimmed());
			}
		}
	}

	private void updatePomFile(Path currentProjectPomPath, List<Result> resultList) throws IOException {
		if (resultList.isEmpty()) {
			logger.debug("No update of pom.xml from from " + this.toMergeProjectPath);
		}
		for (Result result : resultList) {
			// write updated file.
			try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(currentProjectPomPath)) {
				sourceFileWriter.write(result.getAfter().printAllTrimmed());
			}
		}
	}

	private ExecutionContext getExecutionContext() {
		Consumer<Throwable> onError = e -> {
			logger.error("error in javaParser execution", e);
		};
		return new InMemoryExecutionContext(onError);
	}

	public static AddManagedDependency getRecipeAddManagedDependency(String groupId, String artifactId, String version, String scope, String type, String classifier) {
		return new AddManagedDependency(groupId, artifactId, version, scope, type, classifier,
				null, null, null, true);
	}
	public static AddSimpleDependencyRecipe getRecipeAddDependency(String groupId, String artifactId, String version, String scope, String onlyIfUsing) {

		return new AddSimpleDependencyRecipe(groupId, artifactId, version, null, scope, true, onlyIfUsing, null, null, false, null);
	}

}

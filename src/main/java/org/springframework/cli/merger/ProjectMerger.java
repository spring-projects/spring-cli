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
import java.nio.charset.StandardCharsets;
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
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.Java17Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.AddRepository;
import org.openrewrite.maven.ChangePropertyValue;
import org.openrewrite.maven.MavenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor.ResolutionMethod;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.recipe.AddManagedDependencyRecipeFactory;
import org.springframework.cli.util.PomReader;
import org.springframework.cli.util.PropertyFileUtils;
import org.springframework.cli.util.RefactorUtils;
import org.springframework.cli.util.RootPackageFinder;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.FileSystemResource;

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
	 * @param toMergeProjectPath The Path where the new project to merge is located
	 * @param currentProjectPath The Path where the current project is located
	 * @param projectName used to change the name of README files
	 * @param terminalMessage terminal to write user messages to
	 */
	public ProjectMerger(Path toMergeProjectPath, Path currentProjectPath, String projectName,
			TerminalMessage terminalMessage) {
		this.toMergeProjectPath = toMergeProjectPath;
		this.currentProjectPath = currentProjectPath;
		this.projectName = projectName;
		this.terminalMessage = terminalMessage;
	}

	public void merge() {
		PomReader pomReader = new PomReader();
		Path toMergeProjectPomPath = this.toMergeProjectPath.resolve("pom.xml");
		if (Files.notExists(toMergeProjectPomPath)) {
			// only do the copy of files
			try {
				copyToMergeCodebase();
				return;
			}
			catch (IOException ex) {
				throw new SpringCliException(
						"Error copying files from to be merged project.  Error Message = " + ex.getMessage(), ex);
			}
		}
		Path currentProjectPomPath = this.currentProjectPath.resolve("pom.xml");
		if (Files.notExists(currentProjectPomPath)) {
			throw new SpringCliException("Could not find pom.xml in " + this.currentProjectPath
					+ ".  Make sure you are running the command in the project's root directory or specify the --path option.");
		}
		Model currentModel = pomReader.readPom(currentProjectPomPath.toFile());
		Model toMergeModel = pomReader.readPom(toMergeProjectPomPath.toFile());

		List<Path> paths = new ArrayList<>();
		paths.add(currentProjectPomPath);
		MavenParser mavenParser = MavenParser.builder().build();

		MergerPreCheck.canMergeProject(currentModel, toMergeModel, this.toMergeProjectPath);

		try {
			// Maven merges
			mergeMavenRepositories(currentProjectPomPath, currentModel, toMergeModel, paths, mavenParser);
			mergeMavenProperties(currentProjectPomPath, toMergeModel);
			mergeMavenDependencyManagement(currentProjectPomPath, toMergeModel, paths, mavenParser);
			mergeMavenDependencies(currentProjectPomPath, currentModel, toMergeModel, paths, mavenParser);

			// Code Refactoring
			refactorToMergeCodebase();
			// Copy and merge files
			copyToMergeCodebase();

			mergeSpringBootApplicationClassAnnotations();
		}
		catch (IOException ex) {
			throw new SpringCliException("Error merging projects.", ex);
		}
	}

	private void mergeSpringBootApplicationClassAnnotations() throws IOException {

		logger.debug("Looking for @SpringBootApplication in directory " + this.toMergeProjectPath.toFile());
		Optional<File> springBootApplicationFile = RootPackageFinder
			.findSpringBootApplicationFile(this.toMergeProjectPath.toFile());

		if (springBootApplicationFile.isPresent()) {
			CollectAnnotationAndImportInformationRecipe collectAnnotationAndImportInformationRecipe = new CollectAnnotationAndImportInformationRecipe();
			Consumer<Throwable> onError = e -> {
				logger.error("error in javaParser execution", e);
			};
			InMemoryExecutionContext executionContext = new InMemoryExecutionContext(onError);
			List<Path> paths = new ArrayList<>();
			paths.add(springBootApplicationFile.get().toPath());
			JavaParser javaParser = new Java17Parser.Builder().build();
			List<SourceFile> compilationUnits = javaParser.parse(paths, null, executionContext).toList();
			collectAnnotationAndImportInformationRecipe.run(new InMemoryLargeSourceSet(compilationUnits),
					executionContext);

			List<Annotation> declaredAnnotations = collectAnnotationAndImportInformationRecipe.getDeclaredAnnotations();
			List<String> declaredImports = collectAnnotationAndImportInformationRecipe.getDeclaredImports();

			Map<String, String> annotationImportMap = new HashMap<>();
			for (Annotation declaredAnnotation : declaredAnnotations) {
				if (declaredAnnotation.toString().startsWith("@SpringBootApplication")) {
					continue;
				}
				for (String declaredImport : declaredImports) {
					// get the import statement that matches the annotation
					if (declaredImport.contains(declaredAnnotation.getSimpleName())) {
						annotationImportMap.put(declaredAnnotation.toString(), declaredImport);
					}
				}
			}

			logger.debug("Looking for @SpringBootApplication in directory " + this.currentProjectPath.toFile());
			Optional<File> currentSpringBootApplicationFile = RootPackageFinder
				.findSpringBootApplicationFile(this.currentProjectPath.toFile());
			if (currentSpringBootApplicationFile.isPresent()) {
				executionContext = new InMemoryExecutionContext(onError);
				paths = new ArrayList<>();
				paths.add(currentSpringBootApplicationFile.get().toPath());
				javaParser = new Java17Parser.Builder().build();
				compilationUnits = javaParser.parse(paths, null, executionContext).toList();
				for (Entry<String, String> annotationImportEntry : annotationImportMap.entrySet()) {
					String annotation = annotationImportEntry.getKey();
					String importStatement = annotationImportEntry.getValue();
					AddImport addImport = new AddImport(importStatement, null, false);
					AddImportRecipe addImportRecipe = new AddImportRecipe(addImport);
					List<Result> results = addImportRecipe
						.run(new InMemoryLargeSourceSet(compilationUnits), executionContext)
						.getChangeset()
						.getAllResults();
					updateSpringApplicationClass(currentSpringBootApplicationFile.get().toPath(), results);

					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging Main Spring Boot Application class annotation: " + annotation);
					terminalMessage.print(sb.toAttributedString());

					injectAnnotation(currentSpringBootApplicationFile.get().toPath(), annotation);
					// AddAnnotationToClassRecipe addAnnotationToClassRecipe = new
					// AddAnnotationToClassRecipe(annotation);
					// results = addAnnotationToClassRecipe.run(compilationUnits);
					// updateSpringApplicationClass(currentSpringBootApplicationFile.get().toPath(),
					// results);
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
			}
			else {
				logger.debug("Did not add annotation" + annotation + " to file " + pathToFile);
			}
		}
		catch (IOException ex) {
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
		ds.setExcludes(new String[] { ".mvn/**", ".idea" });
		ds.scan();
		String[] fileNames = ds.getIncludedFiles();
		Optional<File> springBootApplicationFile = RootPackageFinder
			.findSpringBootApplicationFile(this.toMergeProjectPath.toFile());
		for (String fileName : fileNames) {
			File srcFile = new File(fromDir, fileName);
			File destFile = new File(toDir, fileName);
			if (srcFile.getName().equals("pom.xml") || srcFile.getName().equals("LICENSE")) {
				continue;
			}
			// hack to avoid bringing over any gradle files for now as this POC is maven
			// only.
			if (srcFile.getName().contains("gradle")) {
				continue;
			}
			// Change readme file name have the project name that is being merged into the
			// code base
			if (FilenameUtils.getBaseName(srcFile.getName()).equalsIgnoreCase("README")) {
				Path newFile = Paths.get(FilenameUtils.getPath(destFile.getName()),
						FilenameUtils.getBaseName(destFile.getName()) + "-" + projectName + "."
								+ FilenameUtils.getExtension(destFile.getName()));
				destFile = new File(toDir, newFile.toFile().getName());
			}
			if (springBootApplicationFile.isPresent()) {
				if (srcFile.equals(springBootApplicationFile.get())) {
					continue;
				}
			}
			if (destFile.exists() && srcFile.getName().equals("application")) {
				Optional<String> extension = getExtension(srcFile.getName());
				if (extension.isPresent() && extension.get().equals("properties")) {
					mergeAndWriteProperties(srcFile, destFile);
				}
				else if (extension.isPresent() && (extension.get().equals("yaml") || extension.get().equals("yml"))) {
					mergeAndWriteYaml(srcFile, destFile);
				}
				else {
					logger.debug("WARNING: Not copying file as it already exists: " + srcFile);
				}
			}
			else {
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
		terminalMessage.print(sb.toAttributedString());

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
		Properties mergedProperties = PropertyFileUtils.mergeProperties(srcProperties, destProperties);
		// look into handling a merge of maven-wrapper.properties - should only merge
		// using latest versions.
		if (!mergedProperties.equals(srcProperties)) {

			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.WHITE));
			sb.append("Merging Spring Application property file...");
			terminalMessage.print(sb.toAttributedString());

			mergedProperties.store(new FileWriter(destFile), "updated by spring cli");
		}
	}

	public Optional<String> getExtension(String filename) {
		return Optional.ofNullable(filename)
			.filter(f -> f.contains("."))
			.map(f -> f.substring(filename.lastIndexOf(".") + 1));
	}

	private void refactorToMergeCodebase() {

		logger.debug("Looking for @SpringBootApplication in directory " + this.currentProjectPath.toFile());
		Optional<String> currentRootPackageName = RootPackageFinder.findRootPackage(this.currentProjectPath.toFile());
		boolean foundRootPackage = true;
		if (currentRootPackageName.isEmpty()) {
			foundRootPackage = false;
		}

		logger.debug("Looking for @SpringBootApplication in directory " + this.toMergeProjectPath.toFile());
		Optional<String> toMergeRootPackageName = RootPackageFinder.findRootPackage(this.toMergeProjectPath.toFile());
		if (toMergeRootPackageName.isEmpty()) {
			foundRootPackage = false;
		}

		if (foundRootPackage) {
			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.WHITE));
			sb.append("Refactoring code base that is to be merged to package name " + currentRootPackageName.get());
			terminalMessage.print(sb.toAttributedString());
			RefactorUtils.refactorPackage(currentRootPackageName.get(), toMergeRootPackageName.get(),
					this.toMergeProjectPath);
			logger.debug("look in " + this.toMergeProjectPath
					+ " to see if refactoring of 'to merge code base' was done correctly");
		}
		else {
			terminalMessage
				.print("WARNING: Could not find root package containing class with @SpringBootApplication in "
						+ this.currentProjectPath.toFile());
		}
	}

	private void mergeMavenDependencies(Path currentProjectPomPath, Model currentModel, Model toMergeModel,
			List<Path> paths, MavenParser mavenParser) throws IOException {
		logger.debug("mergeMavenDependencies: Merging Maven Dependencies...");
		List<Dependency> toMergeModelDependencies = toMergeModel.getDependencies();
		List<Dependency> currentDependencies = currentModel.getDependencies();

		for (Dependency candidateDependency : toMergeModelDependencies) {
			if (candidateDependencyAlreadyPresent(candidateDependency, currentDependencies)) {
				logger.debug("mergeMavenDependencies: Not merging dependency " + candidateDependency);
			}
			else {
				List<SourceFile> parsedPomFiles = mavenParser
					.parse(paths, this.currentProjectPath, getExecutionContext())
					.toList();
				String scope = candidateDependency.getScope();
				if (scope == null) {
					scope = "compile";
				}
				String version = (candidateDependency.getVersion() == null) ? "latest"
						: candidateDependency.getVersion();
				@Nullable
				String versionPattern = ".*";
				@Nullable
				String type = null;
				@Nullable
				Boolean optional = null;
				@Nullable
				String classifier = null;
				@Nullable
				Pattern familyRegex = null;
				@Nullable
				Boolean acceptTransitive = false;
				Recipe addDependency = getRecipeAddDependency(candidateDependency.getGroupId(),
						candidateDependency.getArtifactId(), version, scope,
						"org.springframework.boot.SpringApplication", versionPattern, type, classifier, optional,
						familyRegex, acceptTransitive);

				List<Result> resultList = addDependency
					.run(new InMemoryLargeSourceSet(parsedPomFiles), getExecutionContext())
					.getChangeset()
					.getAllResults();
				if (!resultList.isEmpty()) {
					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging dependency " + candidateDependency.getGroupId() + ":"
							+ candidateDependency.getArtifactId());
					terminalMessage.print(sb.toAttributedString());
				}
				updatePomFile(currentProjectPomPath, resultList);
			}
		}
	}

	private boolean candidateDependencyAlreadyPresent(Dependency candidateDependency,
			List<Dependency> currentDependencies) {
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

	private boolean candidateRepositoryAlreadyPresent(Repository candidateRepository,
			List<Repository> currentRepositories) {
		String candidateRepositoryId = candidateRepository.getId();
		boolean candidateRepositoryIdAlreadyPresent = false;
		for (Repository currentRepository : currentRepositories) {
			String currentRepositoryId = currentRepository.getId();
			if (candidateRepositoryId.equals(currentRepositoryId)) {
				candidateRepositoryIdAlreadyPresent = true;
				break;
			}
		}
		return candidateRepositoryIdAlreadyPresent;
	}

	private void mergeMavenDependencyManagement(Path currentProjectPomPath, Model modelToMerge, List<Path> paths,
			MavenParser mavenParser) throws IOException {
		DependencyManagement dependencyManagement = modelToMerge.getDependencyManagement();
		if (dependencyManagement != null) {
			List<Dependency> dependencies = dependencyManagement.getDependencies();

			for (Dependency dependency : dependencies) {
				List<SourceFile> pomFiles = mavenParser.parse(paths, this.currentProjectPath, getExecutionContext())
					.toList();
				Recipe addManagedDependency = new AddManagedDependencyRecipeFactory().create(dependency);
				List<Result> resultList = addManagedDependency
					.run(new InMemoryLargeSourceSet(pomFiles), getExecutionContext())
					.getChangeset()
					.getAllResults();
				if (!resultList.isEmpty()) {
					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging dependency management section " + dependency.getGroupId() + ":"
							+ dependency.getArtifactId());
					terminalMessage.print(sb.toAttributedString());
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
			ChangePropertyValue changePropertyValueRecipe = new ChangePropertyValue(keyToMerge,
					propertiesToMerge.getProperty(keyToMerge), true, false);
			// TODO - parse is expensive call, move out of loop?
			List<SourceFile> pomFiles = mavenParser.parse(paths, this.currentProjectPath, getExecutionContext())
				.toList();
			RecipeRun recipeRun = changePropertyValueRecipe.run(new InMemoryLargeSourceSet(pomFiles),
					getExecutionContext());
			List<Result> resultList = recipeRun.getChangeset().getAllResults();
			if (!resultList.isEmpty()) {
				AttributedStringBuilder sb = new AttributedStringBuilder();
				sb.style(sb.style().foreground(AttributedStyle.WHITE));
				sb.append("Merging maven property key " + keyToMerge);
				terminalMessage.print(sb.toAttributedString());
			}
			updatePomFile(currentProjectPomPath, resultList);

		}
	}

	private void mergeMavenRepositories(Path currentProjectPomPath, Model currentModel, Model toMergeModel,
			List<Path> paths, MavenParser mavenParser) throws IOException {
		logger.debug("mergeMavenRepositories: Merging Maven Repositories...");
		List<Repository> toMergeRepositories = toMergeModel.getRepositories();
		List<Repository> currentRepositories = currentModel.getRepositories();
		for (Repository candidateRepository : toMergeRepositories) {
			if (candidateRepositoryAlreadyPresent(candidateRepository, currentRepositories)) {
				logger.debug("mergeMavenDependencies: Not merging repository " + candidateRepository);
			}
			else {
				AddRepository recipeAddRepository = getRecipeAddRepository(candidateRepository.getId(),
						candidateRepository.getUrl(), candidateRepository.getName(), false, false);
				List<SourceFile> pomFiles = mavenParser.parse(paths, this.currentProjectPath, getExecutionContext())
					.toList();
				List<Result> resultList = recipeAddRepository
					.run(new InMemoryLargeSourceSet(pomFiles), getExecutionContext())
					.getChangeset()
					.getAllResults();
				if (!resultList.isEmpty()) {
					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.style(sb.style().foreground(AttributedStyle.WHITE));
					sb.append("Merging repository section " + candidateRepository.getId() + ", "
							+ candidateRepository.getUrl());
					terminalMessage.print(sb.toAttributedString());
				}
				updatePomFile(currentProjectPomPath, resultList);
			}
		}
	}

	private void updateSpringApplicationClass(Path pathToCurrentSpringApplicationClass, List<Result> resultList)
			throws IOException {
		if (resultList.isEmpty()) {
			logger.debug("No update of SpringApplication class in " + pathToCurrentSpringApplicationClass);
		}

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.WHITE));
		sb.append("Adding import statements and annotations to @SpringApplication class");
		terminalMessage.print(sb.toAttributedString());

		for (Result result : resultList) {
			// write updated file.
			try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(pathToCurrentSpringApplicationClass)) {
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
			try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(currentProjectPomPath,
					StandardCharsets.UTF_8)) {
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

	public static AddManagedDependency getRecipeAddManagedDependency(String groupId, String artifactId, String version,
			String scope, String type, String classifier) {
		return new AddManagedDependency(groupId, artifactId, version, scope, type, classifier, null, null, null, true);
	}

	public static Recipe getRecipeAddDependency(String groupId, String artifactId, String version, String scope,
			String onlyIfUsing, @Nullable String versionPattern, @Nullable String type, @Nullable String classifier,
			@Nullable Boolean optional, @Nullable Pattern familyRegex, @Nullable Boolean acceptTransitive) {
		return new Recipe() {
			@Override
			public String getDisplayName() {
				return "Add dependency";
			}

			@Override
			public String getDescription() {
				return getDisplayName();
			}

			@Override
			public TreeVisitor<?, ExecutionContext> getVisitor() {
				return new AddDependencyVisitor(groupId, artifactId, version, versionPattern, scope, false, type,
						classifier, optional, familyRegex);
			}
		};

	}

	public static AddRepository getRecipeAddRepository(String id, String url, String name, boolean snapshotsEnabled,
			boolean releasesEnabled) {
		return new AddRepository(id, url, name, null, snapshotsEnabled, null, null, releasesEnabled, null, null);
	}

}

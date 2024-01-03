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

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.AddManagedDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is a WIP class that tried to use the default AddDependency recipe from OpenRewrite.
 */
public class ProjectMergerWithMavenDepRecipe {

	private static final Logger logger = LoggerFactory.getLogger(ProjectMergerWithMavenDepRecipe.class);

	private Path pathToMerge;

	private Path pathCurrentProject;

	/**
	 *
	 * @param pathToMerge Path of the project we want to merge into the current project
	 */
	public ProjectMergerWithMavenDepRecipe(Path pathToMerge, Path pathCurrentProject) {
		this.pathToMerge = pathToMerge;
		this.pathCurrentProject = pathCurrentProject;
	}

	/**
	 * FIXME: What does this method? Parsing JavaSources without classpath will prevent type resolution.
	 * FIXME: Parsing pom files without reading settings.xml will fail if private repo is used. Can we reuse functionality from spring-rewrite-commons instead?
	 */

	/**
	 * this approach does not work in our use case since it needs to construct a classpath for the application, that is the 'spring up shell'.
	 * The rationale is that the recpie does not want to add a dependency, in particular in a multi-module project, to module that does not
	 * use the code.  This involves loading classes from the classpath.  fall back to simple XML manipulation.
	 */


	/* FIXME: Code has no usages
	public void merge() throws IOException {
		PomReader pomReader = new PomReader();
		Path pomToMerge = this.pathToMerge.resolve("pom.xml");
		if (pomToMerge == null) {
			throw new SpringCliException("Could not find pom.xml in " + this.pathToMerge);
		}
		Path currentProjectPomPath = this.pathCurrentProject.resolve("pom.xml");
		if (Files.notExists(currentProjectPomPath)) {
			throw new SpringCliException("Could not find pom.xml in " + this.pathCurrentProject);
		}
		Model modelToMerge = pomReader.readPom(pomToMerge.toFile());

		// Set up recipe arguments

		List<Path> paths = new ArrayList<>();
		paths.add(currentProjectPomPath);
		MavenParser mavenParser = MavenParser.builder().build();

		// Properties section

		Properties propertiesToMerge = modelToMerge.getProperties();
		Set<String> keysToMerge = propertiesToMerge.stringPropertyNames();
		System.out.println("\nMering Maven Properties...");
		for (String keyToMerge : keysToMerge) {
			// TODO may want to do something special in case java.version is set to be different
			//System.out.println("Going to merge property key " + keyToMerge);
			ChangePropertyValue changePropertyValueRecipe = new ChangePropertyValue(keyToMerge, propertiesToMerge.getProperty(keyToMerge), true, false);
			List<SourceFile> pomFiles = mavenParser.parse(paths, this.pathCurrentProject, getExecutionContext()).toList();
			List<Result> resultList = changePropertyValueRecipe.run(new InMemoryLargeSourceSet(pomFiles), getExecutionContext()).getChangeset().getAllResults();
			updatePomFile(currentProjectPomPath, resultList);
 		}

		// Dependency Management Section
		DependencyManagement dependencyManagement = modelToMerge.getDependencyManagement();
		List<Dependency> dependencies = dependencyManagement.getDependencies();

		System.out.println("\nMerging Maven Dependency Management");
		for (Dependency dependency : dependencies) {
			//System.out.println("Going to try and add dependency management section for " + dependency);
			AddManagedDependency addManagedDependency = getRecipeAddManagedDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(),
					dependency.getType(), dependency.getClassifier());

			List<SourceFile> pomFiles = mavenParser.parse(paths, this.pathCurrentProject, getExecutionContext()).toList();
			List<Result> resultList = addManagedDependency.run(new InMemoryLargeSourceSet(pomFiles), getExecutionContext()).getChangeset().getAllResults();
			updatePomFile(currentProjectPomPath, resultList);
		}

		// Dependencies Section



		List<Dependency> dependenciesToMerge = modelToMerge.getDependencies();

		// Need to get source files
		JavaParser javaParser = JavaParser.fromJavaVersion()
				.logCompilationWarningsAndErrors(true)
				.classpath("spring-boot")
				.build();

		// FIXME: How to deal wih additional parsing here?

//		javaParser.setSourceSet("main");

		FileTypeCollectingFileVisitor collector = new FileTypeCollectingFileVisitor(".java");
		try {
			Files.walkFileTree(this.pathCurrentProject, collector);
		}
		catch (IOException e) {
			throw new SpringCliException("Failed reading files in " + this.pathCurrentProject, e);
		}

//		List<? extends SourceFile> compilationUnits =
		JavaProject javaProject = new JavaProject(Tree.randomId(), "myproject", null);



		List<CompilationUnit> parsedJavaFiles = javaParser.parse(
				collector.getMatches(),
				null,
				getExecutionContext());
		List<CompilationUnit> parsedJavaFilesToUse = parsedJavaFiles.stream().map(j -> j.withMarkers(j.getMarkers().addIfAbsent(javaProject))).collect(Collectors.toList());


		System.out.println("--- Java Files Start ---");
		for (CompilationUnit compilationUnit : parsedJavaFiles) {
			System.out.println(compilationUnit.printAllTrimmed());
		}
		System.out.println("--- Java Files End ---");

		//List<? extends SourceFile> allSourceFiles;
		List<Document> parsedPomFiles = mavenParser.parse(paths, this.pathCurrentProject, getExecutionContext());
		List<Document> parsedPomFilesToUse = parsedPomFiles.stream().map(j -> j.withMarkers(j.getMarkers().addIfAbsent(javaProject))).collect(Collectors.toList());

		//Collections.copy(allSourceFiles, compilationUnits);
		List untyped = (List)parsedJavaFilesToUse;
		untyped.addAll(parsedPomFilesToUse);

		System.out.println("\nMerging Maven Dependencies...");
		for (Dependency dependency : dependenciesToMerge) {
			String scope = dependency.getScope();
			if (scope == null) {
				scope = "compile";
			}
			//System.out.println("Going to try and add dependency for " + dependency + " scope = " + scope);
			AddDependency addDependency = getRecipeAddDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null, "org.springframework.boot.SpringApplication");


			List<Result> resultList = addDependency.run(new InMemoryLargeSourceSet(untyped), getExecutionContext()).getChangeset().getAllResults();
			updatePomFile(currentProjectPomPath, resultList);
		}
	}
 */

	private void updatePomFile(Path currentProjectPomPath, List<Result> resultList) throws IOException {
		if (resultList.isEmpty()) {
			System.out.println("No update of pom.xml from from " + this.pathToMerge);
		}
		for (Result result : resultList) {
			// write updated file.
			try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(currentProjectPomPath)) {
				System.out.println("Updating pom.xml in " + currentProjectPomPath);
				sourceFileWriter.write(result.getAfter().printAllTrimmed());
			}
		}
	}

	private ExecutionContext getExecutionContext() {
		Consumer<Throwable> onError = e -> {
			logger.error("error in javaParser execution", e);
		};
		InMemoryExecutionContext executionContext = new InMemoryExecutionContext(onError);
		executionContext.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
		return executionContext;
	}

	private AddManagedDependency getRecipeAddManagedDependency(String groupId, String artifactId, String version, String scope, String type, String classifier) {
		return new AddManagedDependency(groupId, artifactId, version, scope, type, classifier,
				null, null, null, true);
	}

	private AddDependency getRecipeAddDependency(String groupId, String artifactId, String version, String scope, String onlyIfUsing) {
		@Nullable Boolean acceptTransitive = true;
		return new AddDependency(groupId, artifactId, version, null, scope, true, onlyIfUsing, null, null, false, null, acceptTransitive);
	}

}

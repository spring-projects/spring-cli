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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.MavenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.util.PomReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Low level test of performing a merge of a maven pom file
 */
public class MavenModificationTests {

	private static final Logger logger = LoggerFactory.getLogger(MavenModificationTests.class);

	@Test
	void addManagedDependency(@TempDir Path tempDir) throws IOException {
		MavenParser mavenParser = MavenParser.builder().build();

		Path mergedPomPath = tempDir.resolve("pom-merged-project.xml");
		Path pomExisting = Paths.get("src/test/resources/pom-existing-project.xml");

		// move existing contents into a pom.xml in the temp dir
		List<String> pomContents = Files.readAllLines(pomExisting);
		Files.write(mergedPomPath, pomContents);


		PomReader pomReader = new PomReader();
		Path pomToMerge = Paths.get("src/test/resources/pom-project-to-add.xml");
		Model modelToMerge = pomReader.readPom(pomToMerge.toFile());

		DependencyManagement dependencyManagement = modelToMerge.getDependencyManagement();
		List<Dependency> toMergeDependencyManagementDependencies = dependencyManagement.getDependencies();

		Consumer<Throwable> onError = e -> {
			logger.error("error in javaParser execution", e);
		};
		InMemoryExecutionContext executionContext = new InMemoryExecutionContext(onError);
		List<Path> paths = new ArrayList<>();
		paths.add(mergedPomPath);

		for (Dependency dependency : toMergeDependencyManagementDependencies) {
			AddManagedDependency addManagedDependency = ProjectMerger.getRecipeAddManagedDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(),
					dependency.getType(), dependency.getClassifier());

			List<? extends SourceFile> pomFiles = mavenParser.parse(paths, tempDir, executionContext);
			List<Result> resultList = addManagedDependency.run(pomFiles);

			assertThat(resultList.size()).isEqualTo(1);
			for (Result result : resultList) {
				System.out.println("-------- result -----------");
				System.out.println(result.getAfter().printAllTrimmed());

				PomReader mergedPomReader = new PomReader();
				Path mergedPom = Paths.get("src/test/resources/pom-project-to-add.xml");
				Model mergedModel = mergedPomReader.readPom(mergedPom.toFile());
				DependencyManagement mergedDependencyManagement = mergedModel.getDependencyManagement();
				List<Dependency> mergedDependencyManagementDependencies = mergedDependencyManagement.getDependencies();
				assertThat(mergedDependencyManagementDependencies.size()).isEqualTo(1);
				assertThat(mergedDependencyManagementDependencies.get(0).getGroupId()).isEqualTo("org.springframework.cloud");
				assertThat(mergedDependencyManagementDependencies.get(0).getArtifactId()).isEqualTo("spring-cloud-dependencies");
				assertThat(mergedDependencyManagementDependencies.get(0).getVersion()).isEqualTo("2021.0.0");
			}
		}
	}


//	private AddManagedDependency getRecipeAddManagedDependency(String groupId, String artifactId, String version, String scope, String type, String classifier) {
//		return new AddManagedDependency(groupId, artifactId, version, scope, type, classifier,
//				null, null, null, true);
//	}

}

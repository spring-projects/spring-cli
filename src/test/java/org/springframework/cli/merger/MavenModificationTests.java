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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.MavenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.util.ConversionUtils;
import org.springframework.cli.util.PomReader;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Low level test of performing a merge of a maven pom file
 */
public class MavenModificationTests {

	private static final Logger logger = LoggerFactory.getLogger(MavenModificationTests.class);

	@Test
	void addPluginDependency(@TempDir Path tempDir) throws Exception {
		MavenParser mavenParser = MavenParser.builder().build();

		Path mergedPomPath = tempDir.resolve("temp-existing-pom.xml");
		Path pomExisting = Paths.get("src/test/resources/pom-existing-project.xml");

		// move existing contents into a pom.xml in the temp dir
		Files.write(mergedPomPath, Files.readAllLines(pomExisting));

		PomReader pomReader = new PomReader();
		Path pomToMerge = Paths.get("src/test/resources/pom-project-to-add.xml");

		ProjectMerger merger = new ProjectMerger(tempDir.resolve("to"), tempDir.resolve("from"), "foo-project", null);
		Method mergeMavenPlugins = ReflectionUtils.findMethod(ProjectMerger.class, "mergeMavenPlugins", Path.class,
				Model.class, Model.class, List.class, MavenParser.class);
		mergeMavenPlugins.setAccessible(true);

		List<Path> paths = new ArrayList<>();
		paths.add(mergedPomPath);
		mergeMavenPlugins.invoke(merger, mergedPomPath, pomReader.readPom(pomExisting.toFile()),
				pomReader.readPom(pomToMerge.toFile()), paths, mavenParser);

		Model mergedModel = pomReader.readPom(mergedPomPath.toFile());
		for (Plugin plugin : mergedModel.getBuild().getPlugins()) {
			if (plugin.getGroupId().equals("org.apache.maven.plugins")
					&& plugin.getArtifactId().equals("maven-deploy-plugin")) {
				assertThat(ConversionUtils.fromDomToString((Xpp3Dom) plugin.getConfiguration()))
					.contains("<skip>true</skip>");
			}
			else if (plugin.getGroupId().equals("org.apache.maven.plugins")
					&& plugin.getArtifactId().equals("maven-shade-plugin")) {
				String configurationXML = ConversionUtils.fromDomToString(((Xpp3Dom) plugin.getConfiguration()));
				assertThat(configurationXML).contains("<createDependencyReducedPom>false</createDependencyReducedPom>");
				assertThat(configurationXML).contains("<shadedArtifactAttached>true</shadedArtifactAttached>");
				assertThat(configurationXML).contains("<shadedClassifierName>aws</shadedClassifierName>");
			}
			else if (plugin.getGroupId().equals("org.springframework.boot")
					&& plugin.getArtifactId().equals("spring-boot-maven-plugin")) {
				Dependency dep = plugin.getDependencies().iterator().next();
				assertThat(dep.getGroupId()).isEqualTo("org.springframework.boot.experimental");
				assertThat(dep.getArtifactId()).isEqualTo("spring-boot-thin-layout");
			}
		}
	}

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
			AddManagedDependency addManagedDependency = ProjectMerger.getRecipeAddManagedDependency(
					dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(),
					dependency.getType(), dependency.getClassifier());

			List<SourceFile> pomFiles = mavenParser.parse(paths, tempDir, executionContext).toList();
			List<Result> resultList = addManagedDependency.run(new InMemoryLargeSourceSet(pomFiles), executionContext)
				.getChangeset()
				.getAllResults();

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
				assertThat(mergedDependencyManagementDependencies.get(0).getGroupId())
					.isEqualTo("org.springframework.cloud");
				assertThat(mergedDependencyManagementDependencies.get(0).getArtifactId())
					.isEqualTo("spring-cloud-dependencies");
				assertThat(mergedDependencyManagementDependencies.get(0).getVersion()).isEqualTo("2021.0.0");
			}
		}
	}

	// private AddManagedDependency getRecipeAddManagedDependency(String groupId, String
	// artifactId, String version, String scope, String type, String classifier) {
	// return new AddManagedDependency(groupId, artifactId, version, scope, type,
	// classifier,
	// null, null, null, true);
	// }

}

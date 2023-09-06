/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.cli.command;

import java.nio.file.Path;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.junitpioneer.jupiter.ExpectedToFail;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockFakeUserConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;
import org.springframework.cli.util.PomReader;
import org.springframework.sbm.boot.autoconfigure.SbmSupportRewriteConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

public class BootCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockBaseConfig.class, SbmSupportRewriteConfiguration.class);

	@Test
	void canCreateFromDefaults(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);

			String path = workingDir.toAbsolutePath().toString();
			bootCommands.bootNew("rest-service", null, null, null, null, null, null, path);
			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("rest-service")).exists();
			assertThat(workingDir.resolve("rest-service/src/main/java/com/example/restservice/greeting")).exists();
			assertThat(workingDir.resolve("rest-service/src/test/java/com/example/restservice/greeting")).exists();
		});
	}

	@Test
	void canCreateAndChangeNameAndPackageBasedOnGroupId(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("demo2", null, "com.xkcd", null, null, null, null, path);
			assertThat(workingDir.resolve("demo2")).exists();
			assertThat(workingDir.resolve("demo2/src/main/java/com/xkcd/demo2/greeting")).exists();
			assertThat(workingDir.resolve("demo2/src/test/java/com/xkcd/demo2/greeting")).exists();
		});
	}

	@Test
	@Disabled("FIXME: Sometimes runs endless")
	void canCreateAndChangeNameAndPackageBasedOnPackageName(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("demo2", null, null, null, null, null, "com.xkcd", path);
			assertThat(workingDir.resolve("demo2")).exists();
			assertThat(workingDir.resolve("demo2/src/main/java/com/xkcd/greeting")).exists();
			assertThat(workingDir.resolve("demo2/src/test/java/com/xkcd/greeting")).exists();
		});
	}

	@Test
	void canCreateUsingGithubUri(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("jpa", "https://github.com/rd-1-2022/rpt-spring-data-jpa", null, null, null, null, "com.xkcd", path);
			assertThat(workingDir.resolve("jpa")).exists();
			assertThat(workingDir.resolve("jpa/src/main/java/com/xkcd/customer")).exists();
			assertThat(workingDir.resolve("jpa/src/test/java/com/xkcd/customer")).exists();
		});
	}

	@Test
	void canCreateFromRegisteredProject(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockFakeUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("jpa2", "jpa", null, null, null, null, "com.xkcd", path);
			assertThat(workingDir.resolve("jpa2")).exists();
			assertThat(workingDir.resolve("jpa2/src/main/java/com/xkcd/customer")).exists();
			assertThat(workingDir.resolve("jpa2/src/test/java/com/xkcd/customer")).exists();
		});
	}

	@Test
	void canCreateFromRegisteredCatalog(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockFakeUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("scheduling", "scheduling", null, null, null, null, "com.xkcd", path);
			assertThat(workingDir.resolve("scheduling")).exists();
			assertThat(workingDir.resolve("scheduling/src/main/java/com/xkcd/scheduling")).exists();
			assertThat(workingDir.resolve("scheduling/src/test/java/com/xkcd/scheduling")).exists();
		});
	}

	@Test
	void canCreateAndAddProjects(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockFakeUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("test-add", null,null, null, null, null, "com.xkcd", path);
			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("test-add")).exists();
			assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/greeting")).exists();
			assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/greeting")).exists();

			String addPath = workingDir.resolve("test-add").toAbsolutePath().toString();
			bootCommands.bootAdd("https://github.com/rd-1-2022/rpt-spring-data-jpa", addPath);
			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/customer")).exists();
			assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/customer")).exists();

			bootCommands.bootAdd("scheduling", addPath);
			assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/scheduling")).exists();
			assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/scheduling")).exists();
		});
	}

	@Test
	@ExpectedToFail("FIXME: dependencyManagement is null in line 184")
	@Disabled("FIXME: Sometimes runs endless")
	void canCreateAndAddProjectThatModifiesManagedDepsAndMergesProperties(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockFakeUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew("test-add", null,  null, null, null, null, "com.xkcd", path);
			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("test-add")).exists();
			assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/greeting")).exists();
			assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/greeting")).exists();

			String addPath = workingDir.resolve("test-add").toAbsolutePath().toString();

			PomReader pomReader = new PomReader();
			Model model = pomReader.readPom(workingDir.resolve("test-add/pom.xml").toFile());
			assertThat(model.getProperties()).doesNotContainKey("spring-cloud.version");
			DependencyManagement dependencyManagement = model.getDependencyManagement();
			assertThat(dependencyManagement).isNull();

			bootCommands.bootAdd("https://github.com/rd-1-2022/rpt-config-client", addPath);

			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/controller")).exists();
			assertThat(workingDir.resolve("test-add/README-rpt-config-client.md")).exists();

			model = pomReader.readPom(workingDir.resolve("test-add/pom.xml").toFile());
			assertThat(model.getProperties()).containsKey("spring-cloud.version");
			dependencyManagement = model.getDependencyManagement();
			assertThat(dependencyManagement.getDependencies().size()).isEqualTo(1);
			assertThat(dependencyManagement.getDependencies().get(0).getGroupId()).isEqualTo("org.springframework.cloud");
			assertThat(dependencyManagement.getDependencies().get(0).getArtifactId()).isEqualTo("spring-cloud-dependencies");
			assertThat(dependencyManagement.getDependencies().get(0).getVersion()).isEqualTo("${spring-cloud.version}");
			assertThat(dependencyManagement.getDependencies().get(0).getType()).isEqualTo("pom");
			assertThat(dependencyManagement.getDependencies().get(0).getScope()).isEqualTo("import");


		});
	}

}

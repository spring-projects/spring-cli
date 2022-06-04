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


package org.springframework.cli.command;

import java.nio.file.Path;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.springframework.cli.git.GitSourceRepositoryService;
import org.springframework.cli.support.SpringCliUserConfig;
import org.springframework.cli.util.IoUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cli.support.SpringCliUserConfig.SPRING_CLI_CONFIG_DIR;

@ExtendWith(SystemStubsExtension.class)
public class BootCommandsTests {

	private ListAssert<String> filesAssert;

	@SystemStub
	private EnvironmentVariables environmentVariables;

	@Test
	void testBootNew(final @TempDir Path configDir, final @TempDir Path workingDir) {
		environmentVariables.set(IoUtils.TEST_WORKING_DIRECTORY, workingDir.toAbsolutePath().toString());
		environmentVariables.set(SPRING_CLI_CONFIG_DIR, configDir.toAbsolutePath().toString());
		SpringCliUserConfig springCliUserConfig = new SpringCliUserConfig();
		GitSourceRepositoryService gitSourceRepositoryService = new GitSourceRepositoryService(springCliUserConfig);
		BootCommands bootCommands = new BootCommands(springCliUserConfig, gitSourceRepositoryService);

		// This gets the default project hard-coded into spring-cli
		bootCommands.bootNew(null, null, null);
		assertThat(workingDir).exists().isDirectory();
		assertThat(workingDir.resolve("demo")).exists();
		assertThat(workingDir.resolve("demo/src/main/java/com/example/restservice/greeting")).exists();
		assertThat(workingDir.resolve("demo/src/test/java/com/example/restservice/greeting")).exists();

		bootCommands.bootNew(null, "demo2", "com.xkcd");
		assertThat(workingDir.resolve("demo2")).exists();
		assertThat(workingDir.resolve("demo2/src/main/java/com/xkcd/greeting")).exists();
		assertThat(workingDir.resolve("demo2/src/test/java/com/xkcd/greeting")).exists();

		// Create a project using a github url
		bootCommands.bootNew("https://github.com/rd-1-2022/rpt-spring-data-jpa", "jpa", "com.xkcd");
		assertThat(workingDir.resolve("jpa")).exists();
		assertThat(workingDir.resolve("jpa/src/main/java/com/xkcd/customer")).exists();
		assertThat(workingDir.resolve("jpa/src/test/java/com/xkcd/customer")).exists();

		// Register a project and then create a new project form that one.
		ProjectCommands projectCommands = new ProjectCommands(springCliUserConfig, gitSourceRepositoryService);
		projectCommands.projectAdd("jpa", "https://github.com/rd-1-2022/rpt-spring-data-jpa", "Learn JPA", null);

		bootCommands.bootNew("jpa", "jpa2", "com.xkcd");
		assertThat(workingDir.resolve("jpa2")).exists();
		assertThat(workingDir.resolve("jpa2/src/main/java/com/xkcd/customer")).exists();
		assertThat(workingDir.resolve("jpa2/src/test/java/com/xkcd/customer")).exists();

		// Remove registered project as it will already exist in the catalog.  Not an issue, but just tidying up.
		projectCommands.projectRemove("jpa");

		// Add a catalog and then create a new project from the catalog
		ProjectCatalogCommands projectCatalogCommands = new ProjectCatalogCommands(springCliUserConfig);
		projectCatalogCommands.catalogAdd("getting-started", "https://github.com/rd-1-2022/spring-gs-catalog/", "Spring Getting Started Projects", null);
		bootCommands.bootNew("scheduling", "scheduling", "com.xkcd");
		assertThat(workingDir.resolve("scheduling")).exists();
		assertThat(workingDir.resolve("scheduling/src/main/java/com/xkcd/scheduling")).exists();
		assertThat(workingDir.resolve("scheduling/src/test/java/com/xkcd/scheduling")).exists();

	}

	@Test
	void testBootAdd(final @TempDir Path configDir, final @TempDir Path workingDir) {
		environmentVariables.set(IoUtils.TEST_WORKING_DIRECTORY, workingDir.toAbsolutePath().toString());
		environmentVariables.set(SPRING_CLI_CONFIG_DIR, configDir.toAbsolutePath().toString());
		SpringCliUserConfig springCliUserConfig = new SpringCliUserConfig();
		GitSourceRepositoryService gitSourceRepositoryService = new GitSourceRepositoryService(springCliUserConfig);
		BootCommands bootCommands = new BootCommands(springCliUserConfig, gitSourceRepositoryService);

		bootCommands.bootNew(null, "test-add", "com.xkcd");
		assertThat(workingDir).exists().isDirectory();
		assertThat(workingDir.resolve("test-add")).exists();
		assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/greeting")).exists();
		assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/greeting")).exists();

		// 'CD' into the project's working directory
		environmentVariables.set(IoUtils.TEST_WORKING_DIRECTORY, workingDir.resolve("test-add").toAbsolutePath().toString());

		// Add from a github url
		bootCommands.bootAdd("https://github.com/rd-1-2022/rpt-spring-data-jpa");
		assertThat(workingDir).exists().isDirectory();
		assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/customer")).exists();
		assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/customer")).exists();

		// Register a project and then create a new project form that one.
		ProjectCommands projectCommands = new ProjectCommands(springCliUserConfig, gitSourceRepositoryService);
		projectCommands.projectAdd("scheduling", "https://github.com/rd-1-2022/rpt-spring-scheduling-tasks", "Scheduling", null);

		bootCommands.bootAdd("scheduling");
		assertThat(workingDir.resolve("test-add/src/main/java/com/xkcd/scheduling")).exists();
		assertThat(workingDir.resolve("test-add/src/test/java/com/xkcd/scheduling")).exists();

	}


}

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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.git.GitSourceRepositoryService;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalogs;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepositories;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepository;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BootCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockBaseConfig.class);

	@Test
	void canCreateFromDefaults(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);

			String path = workingDir.toAbsolutePath().toString();
			bootCommands.bootNew(null, null, null, null, null, null, null, path);
			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("rpt-rest-service")).exists();
			assertThat(workingDir.resolve("rpt-rest-service/src/main/java/com/example/restservice/greeting")).exists();
			assertThat(workingDir.resolve("rpt-rest-service/src/test/java/com/example/restservice/greeting")).exists();
		});
	}

	@Test
	void canCreateAndChangeNameAndPackageBasedOnGroupId(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew(null, "demo2", "com.xkcd", null, null, null, null, path);
			assertThat(workingDir.resolve("demo2")).exists();
			assertThat(workingDir.resolve("demo2/src/main/java/com/xkcd/demo2/greeting")).exists();
			assertThat(workingDir.resolve("demo2/src/test/java/com/xkcd/demo2/greeting")).exists();
		});
	}

	@Test
	void canCreateAndChangeNameAndPackageBasedOnPackageName(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BootCommands.class);
			BootCommands bootCommands = context.getBean(BootCommands.class);
			String path = workingDir.toAbsolutePath().toString();

			bootCommands.bootNew(null, "demo2", null, null, null, null, "com.xkcd", path);
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

			bootCommands.bootNew("https://github.com/rd-1-2022/rpt-spring-data-jpa", "jpa", null, null, null, null, "com.xkcd", path);
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

			bootCommands.bootNew("jpa", "jpa2", null, null, null, null, "com.xkcd", path);
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

			bootCommands.bootNew(null, "test-add", null, null, null, null, "com.xkcd", path);
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

	@Configuration
	static class MockBaseConfig {

		@Bean
		Terminal terminal() {
			Terminal mockTerminal = mock(Terminal.class);
			return mockTerminal;
		}

		@Bean
		GitSourceRepositoryService gitSourceRepositoryService(SpringCliUserConfig springCliUserConfig) {
			return new GitSourceRepositoryService(springCliUserConfig);
		}

		@Bean
		BootCommands bootCommands(SpringCliUserConfig springCliUserConfig,
				SourceRepositoryService sourceRepositoryService) {
			BootCommands bootCommands = new BootCommands(springCliUserConfig, sourceRepositoryService);
			bootCommands.setTerminalMessage(TerminalMessage.noop());
			return bootCommands;
		}
	}

	@Configuration
	static class MockUserConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			FileSystem fileSystem = Jimfs.newFileSystem();
			Function<String, Path> pathProvider = (path) -> fileSystem.getPath(path);
			return new SpringCliUserConfig(pathProvider);
		}
	}

	@Configuration
	static class MockFakeUserConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			SpringCliUserConfig mock = mock(SpringCliUserConfig.class);
			ProjectRepository pr1 = ProjectRepository.of("jpa", "Learn JPA",
					"https://github.com/rd-1-2022/rpt-spring-data-jpa", null);
			ProjectRepository pr2 = ProjectRepository.of("scheduling", "Scheduling",
					"https://github.com/rd-1-2022/rpt-spring-scheduling-tasks", null);
			ProjectRepositories prs = new ProjectRepositories();
			prs.setProjectRepositories(Arrays.asList(pr1, pr2));
			when(mock.getProjectRepositories()).thenReturn(prs);
			ProjectCatalogs pcs = new ProjectCatalogs();
			ProjectCatalog pc = ProjectCatalog.of("getting-started", "Spring Getting Started Projects",
					"https://github.com/rd-1-2022/spring-gs-catalog/", null);
			pcs.setProjectCatalogs(Arrays.asList(pc));
			when(mock.getProjectCatalogs()).thenReturn(pcs);
			return mock;
		}
	}
}

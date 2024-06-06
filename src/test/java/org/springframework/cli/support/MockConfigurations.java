/*
 * Copyright 2021-2024 the original author or authors.
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

package org.springframework.cli.support;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
import org.apache.commons.io.FileUtils;
import org.jline.terminal.Terminal;
import org.mockito.Mockito;

import org.springframework.cli.command.BootCommands;
import org.springframework.cli.command.CommandCommands;
import org.springframework.cli.command.RoleCommands;
import org.springframework.cli.command.SpecialCommands;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalogs;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepositories;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepository;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.runtime.engine.model.MavenModelPopulator;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.runtime.engine.model.RootPackageModelPopulator;
import org.springframework.cli.runtime.engine.model.SystemModelPopulator;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.style.ThemeResolver;

public class MockConfigurations {

	@Configuration
	public static class MockBaseConfig {

		@Bean
		Terminal terminal() {
			Terminal mockTerminal = Mockito.mock(Terminal.class);
			return mockTerminal;
		}

		@Bean
		ThemeResolver themeResolver() {
			ThemeResolver mockThemeResolver = Mockito.mock(ThemeResolver.class);
			return mockThemeResolver;
		}

		@Bean
		SourceRepositoryService sourceRepositoryService() {
			return new SourceRepositoryService() {

				@Override
				public Path retrieveRepositoryContents(String sourceRepoUrl) {
					String testData = null;
					if ("https://github.com/rd-1-2022/rest-service".equals(sourceRepoUrl)) {
						testData = "rest-service";
					}
					else if ("https://github.com/rd-1-2022/rpt-spring-data-jpa".equals(sourceRepoUrl)) {
						testData = "spring-data-jpa";
					}
					else if ("https://github.com/rd-1-2022/rpt-spring-scheduling-tasks".equals(sourceRepoUrl)) {
						testData = "spring-scheduling-tasks";
					}
					else if ("https://github.com/rd-1-2022/rpt-config-client".equals(sourceRepoUrl)) {
						testData = "config-client";
					}
					if (testData != null) {
						try {
							Path projectPath = Path.of("test-data").resolve("projects").resolve(testData);
							Path tempPath = Paths.get(FileUtils.getTempDirectory().getAbsolutePath(),
									UUID.randomUUID().toString());
							File tmpdir = Files.createDirectories(tempPath).toFile();
							FileUtils.copyDirectory(projectPath.toFile(), tmpdir);
							return tmpdir.toPath();
						}
						catch (Exception ex) {
							throw new RuntimeException(ex);
						}

					}
					else {
						throw new RuntimeException("Unknown mock for " + sourceRepoUrl);
					}
				}

			};
		}

		@Bean
		SpecialCommands specialCommands() {
			return new SpecialCommands(TerminalMessage.noop());
		}

		@Bean
		BootCommands bootCommands(SpringCliUserConfig springCliUserConfig,
				SourceRepositoryService sourceRepositoryService) {
			BootCommands bootCommands = new BootCommands(springCliUserConfig, sourceRepositoryService,
					TerminalMessage.noop());
			return bootCommands;
		}

		@Bean
		CommandCommands commandCommands(SourceRepositoryService sourceRepositoryService) {
			return new CommandCommands(sourceRepositoryService, TerminalMessage.noop());
		}

		@Bean
		RoleCommands roleCommands() {
			return new RoleCommands(TerminalMessage.noop());
		}

		@Bean
		ModelPopulator systemModelPopulator() {
			return new SystemModelPopulator();
		}

		@Bean
		ModelPopulator mavenModelPopulator() {
			return new MavenModelPopulator();
		}

		@Bean
		ModelPopulator rootPackageModelPopulator() {
			return new RootPackageModelPopulator();
		}

	}

	@Configuration
	public static class MockUserConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			FileSystem fileSystem = Jimfs.newFileSystem();
			Function<String, Path> pathProvider = (path) -> fileSystem.getPath(path);
			return new SpringCliUserConfig(pathProvider);
		}

	}

	@Configuration
	public static class MockFakeUserConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			SpringCliUserConfig mock = Mockito.mock(SpringCliUserConfig.class);
			ProjectRepository pr1 = ProjectRepository.of("jpa", "Learn JPA",
					"https://github.com/rd-1-2022/rpt-spring-data-jpa", null);
			ProjectRepository pr2 = ProjectRepository.of("scheduling", "Scheduling",
					"https://github.com/rd-1-2022/rpt-spring-scheduling-tasks", null);
			ProjectRepositories prs = new ProjectRepositories();
			prs.setProjectRepositories(Arrays.asList(pr1, pr2));
			Mockito.when(mock.getProjectRepositories()).thenReturn(prs);
			ProjectCatalogs pcs = new ProjectCatalogs();
			ProjectCatalog pc = ProjectCatalog.of("getting-started", "Spring Getting Started Projects",
					"https://github.com/rd-1-2022/spring-gs-catalog/", null);
			pcs.setProjectCatalogs(Arrays.asList(pc));
			Mockito.when(mock.getProjectCatalogs()).thenReturn(pcs);
			return mock;
		}

	}

}

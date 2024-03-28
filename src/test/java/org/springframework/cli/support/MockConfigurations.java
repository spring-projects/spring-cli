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

package org.springframework.cli.support;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
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
import org.springframework.cli.git.GitSourceRepositoryService;
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
		GitSourceRepositoryService gitSourceRepositoryService(SpringCliUserConfig springCliUserConfig) {
			return new GitSourceRepositoryService(springCliUserConfig);
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

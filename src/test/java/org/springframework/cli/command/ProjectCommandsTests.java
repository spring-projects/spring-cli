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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.git.GitSourceRepositoryService;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.testutil.TableAssertions;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockBaseConfig.class);

	@Test
	void testProjectCommands() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ProjectCommands.class);
			ProjectCommands projectCommands = context.getBean(ProjectCommands.class);

			// Get empty table, assert header values
			Table table = (Table) projectCommands.projectList(false);
			System.out.println("should be empty table");
			System.out.println(table.render(100));
			assertEmptyProjectListTable(table);

			// Add a project and assert values
			List<String> tags = new ArrayList<>();
			tags.add("data");
			tags.add("jpa");
			projectCommands.projectAdd("jpa", "https://github.com/rd-1-2022/rpt-spring-data-jpa", "Learn JPA", tags);
			table = (Table) projectCommands.projectList(false);
			System.out.println("SHould have 1 entry");
			System.out.println(table.render(100));
			TableAssertions.verifyTableValue(table, 1, 0, "jpa");
			TableAssertions.verifyTableValue(table, 1, 1, "Learn JPA");
			TableAssertions.verifyTableValue(table, 1, 2, "https://github.com/rd-1-2022/rpt-spring-data-jpa");
			TableAssertions.verifyTableValue(table, 1, 3, ""); // This project is part of
																// a catalog
			TableAssertions.verifyTableValue(table, 1, 4, "[data, jpa]");
			assertThat(table.getModel().getRowCount()).isEqualTo(2);

			// Remove project
			projectCommands.projectRemove("jpa");
			table = (Table) projectCommands.projectList(false);
			System.out.println("Should be empty table");
			System.out.println(table.render(100));
			assertThat(table.getModel().getColumnCount()).isEqualTo(5);
			assertThat(table.getModel().getRowCount()).isEqualTo(1);
		});
	}

	public static void assertEmptyProjectListTable(Table table) {
		assertThat(table.getModel().getColumnCount()).isEqualTo(5);
		assertThat(table.getModel().getRowCount()).isEqualTo(1);
		TableAssertions.verifyTableValue(table, 0, 0, "Name");
		TableAssertions.verifyTableValue(table, 0, 1, "Description");
		TableAssertions.verifyTableValue(table, 0, 2, "URL");
		TableAssertions.verifyTableValue(table, 0, 3, "Catalog");
		TableAssertions.verifyTableValue(table, 0, 4, "Tags");
	}

	@Configuration
	static class MockBaseConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			FileSystem fileSystem = Jimfs.newFileSystem();
			Function<String, Path> pathProvider = (path) -> fileSystem.getPath(path);
			return new SpringCliUserConfig(pathProvider);
		}

		@Bean
		GitSourceRepositoryService gitSourceRepositoryService(SpringCliUserConfig springCliUserConfig) {
			return new GitSourceRepositoryService(springCliUserConfig);
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		ProjectCommands projectCommands(SpringCliUserConfig springCliUserConfig,
				SourceRepositoryService sourceRepositoryService, ObjectMapper objectMapper) {
			ProjectCommands projectCommands = new ProjectCommands(springCliUserConfig, sourceRepositoryService,
					TerminalMessage.noop(), objectMapper);
			return projectCommands;
		}

	}

}

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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.springframework.cli.git.GitSourceRepositoryService;
import org.springframework.cli.support.SpringCliUserConfig;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cli.command.ProjectCommandsTests.assertEmptyProjectListTable;
import static org.springframework.cli.support.SpringCliUserConfig.SPRING_CLI_CONFIG_DIR;
import static org.springframework.cli.testutil.TableAssertions.verifyTableValue;

@ExtendWith(SystemStubsExtension.class)
public class ProjectCommandsListTests {

	@SystemStub
	private EnvironmentVariables environmentVariables;

	/**
	 * Tests that the projects included in a project catalog are included in the listing of all projects
	 */
	@Test
	void testList(final @TempDir Path tempDir) {
		environmentVariables.set(SPRING_CLI_CONFIG_DIR, tempDir.toAbsolutePath().toString());
		ProjectCatalogCommands projectCatalogCommands = new ProjectCatalogCommands(new SpringCliUserConfig());
		SpringCliUserConfig springCliUserConfig = new SpringCliUserConfig();
		ProjectCommands projectCommands = new ProjectCommands(springCliUserConfig, new GitSourceRepositoryService(springCliUserConfig));

		// Get empty table, assert header values
		Table table = projectCommands.projectList();
		//System.out.println(table.render(100));
		assertEmptyProjectListTable(table);

		List<String> tags = new ArrayList<>();
		tags.add("spring");
		tags.add("testing");
		projectCatalogCommands.catalogAdd("getting-started", "https://github.com/rd-1-2022/catalog-for-project-tests", "Test Catalog Projects", tags);

		table = projectCommands.projectList();
		System.out.println(table.render(100));
		verifyTableValue(table, 1, 0, "web");
		verifyTableValue(table, 1, 1, "https://github.com/rd-1-2022/rpt-rest-service");
		verifyTableValue(table, 1, 2, "Hello, World RESTful web service.");
		verifyTableValue(table, 1, 3, "getting-started");
		verifyTableValue(table, 1, 4, "[rest, web]");
		assertThat(table.getModel().getRowCount()).isEqualTo(5);
	}
}

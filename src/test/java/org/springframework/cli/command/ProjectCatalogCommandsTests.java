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

import org.springframework.cli.support.SpringCliUserConfig;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cli.support.SpringCliUserConfig.SPRING_CLI_CONFIG_DIR;
import static org.springframework.cli.testutil.TableAssertions.verifyTableValue;

@ExtendWith(SystemStubsExtension.class)
public class ProjectCatalogCommandsTests {

	@SystemStub
	private EnvironmentVariables environmentVariables;

	@Test
	void testProjectCatalogCommands(final @TempDir Path tempDir) {
		environmentVariables.set(SPRING_CLI_CONFIG_DIR, tempDir.toAbsolutePath().toString());
		ProjectCatalogCommands projectCatalogCommands = new ProjectCatalogCommands(new SpringCliUserConfig());

		// Get empty table, assert header values
		Table table = projectCatalogCommands.catalogList();
		//System.out.println(table.render(100));
		assertThat(table.getModel().getColumnCount()).isEqualTo(4);
		assertThat(table.getModel().getRowCount()).isEqualTo(1);
		verifyTableValue(table, 0, 0, "Name");
		verifyTableValue(table, 0, 1, "URL");
		verifyTableValue(table, 0, 2, "Description");
		verifyTableValue(table, 0, 3, "Tags");

		// Add a catalog and assert values
		List<String> tags = new ArrayList<>();
		tags.add("spring");
		tags.add("guide");
		projectCatalogCommands.catalogAdd("getting-started", "https://github.com/rd-1-2022/spring-gs-catalog/", "Spring Getting Started Projects", tags);
		table = projectCatalogCommands.catalogList();
		System.out.println(table.render(100));
		verifyTableValue(table, 1, 0, "getting-started");
		verifyTableValue(table, 1, 1, "https://github.com/rd-1-2022/spring-gs-catalog/");
		verifyTableValue(table, 1, 2, "Spring Getting Started Projects");
		verifyTableValue(table, 1, 3, "[spring, guide]");
		assertThat(table.getModel().getRowCount()).isEqualTo(2);


	}
}

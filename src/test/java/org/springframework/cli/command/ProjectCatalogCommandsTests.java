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

import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.config.SpringCliProjectCatalogProperties;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.cli.testutil.TableAssertions.verifyTableValue;

public class ProjectCatalogCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockBaseConfig.class);

	@Test
	void testProjectCatalogCommands() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ProjectCatalogCommands.class);
			ProjectCatalogCommands projectCatalogCommands = context.getBean(ProjectCatalogCommands.class);

			// Get empty table, assert header values
			verifyEmptyCatalog(projectCatalogCommands);
			Table table;

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

			assertThatThrownBy(() -> {
				projectCatalogCommands.catalogAdd("getting-started", "https://github.com/rd-1-2022/spring-gs-catalog/", "Spring Getting Started Projects", tags);
			}).isInstanceOf(SpringCliException.class)
					.hasMessageContaining("Catalog named getting-started already exists.  Choose another name.");

			// Remove added catalog
			projectCatalogCommands.catalogRemove("getting-started");
			verifyEmptyCatalog(projectCatalogCommands);
		});
	}

	private static void verifyEmptyCatalog(ProjectCatalogCommands projectCatalogCommands) {
		Table table = projectCatalogCommands.catalogList();
		System.out.println(table.render(100));
		assertThat(table.getModel().getColumnCount()).isEqualTo(4);
		assertThat(table.getModel().getRowCount()).isEqualTo(1);
		verifyTableValue(table, 0, 0, "Name");
		verifyTableValue(table, 0, 1, "URL");
		verifyTableValue(table, 0, 2, "Description");
		verifyTableValue(table, 0, 3, "Tags");
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
		SpringCliProjectCatalogProperties springCliProjectCatalogProperties() {
			return new SpringCliProjectCatalogProperties();
		}

		@Bean
		ProjectCatalogCommands projectCatalogCommands(SpringCliUserConfig springCliUserConfig, SpringCliProjectCatalogProperties springCliProjectCatalogProperties) {
			ProjectCatalogCommands projectCatalogCommands = new ProjectCatalogCommands(springCliUserConfig);
			return projectCatalogCommands;
		}
	}
}

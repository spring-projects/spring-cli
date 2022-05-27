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

@ExtendWith(SystemStubsExtension.class)
public class ConfigCommandTests {
	@SystemStub
	private EnvironmentVariables environmentVariables;


	@Test
	void testConfigCommands(final @TempDir Path tempDir) {

		environmentVariables.set(SPRING_CLI_CONFIG_DIR, tempDir.toAbsolutePath().toString());
		ConfigCommands configCommands = new ConfigCommands(new SpringCliUserConfig());

		Table table = configCommands.configList();
		//System.out.println(table.render(80));
		assertThat(table.getModel().getColumnCount()).isEqualTo(3);
		assertThat(table.getModel().getRowCount()).isEqualTo(1);
		assertThat(table.getModel().getValue(0,0)).
				as("Header Row, First Column value should be: Command")
				.isEqualTo("Command");
		assertThat(table.getModel().getValue(0,1)).
				as("Header Row, Second Column value should be: Sub Command")
				.isEqualTo("Sub Command");
		assertThat(table.getModel().getValue(0,2)).
				as("Header Row, Third Column value should be Option Name/Values")
				.isEqualTo("Option Name/Values");

		configCommands.configSet("boot", "new", "package-name", "com.xkcd");
		table = configCommands.configList();
		//System.out.println(table.render(80));
		assertThat(table.getModel().getRowCount()).isEqualTo(2);
		assertThat(table.getModel().getValue(1,0)).
				as("First Row, First Column value should be: boot")
				.isEqualTo("boot");
		assertThat(table.getModel().getValue(1,1)).
				as("First Row, Second Column value should be: new")
				.isEqualTo("new");
		assertThat(table.getModel().getValue(1,2)).
				as("First Row, Third Column value should be Option Name/Values")
				.isEqualTo("['package-name' = 'com.xkcd']");

		configCommands.configUnSet("boot", "new", "package-name");
		table = configCommands.configList();
		System.out.println(table.render(80));

		assertThat(table.getModel().getColumnCount()).isEqualTo(3);
		assertThat(table.getModel().getRowCount()).isEqualTo(1);
	}

	private void verifyTableValue(Table table, int row, int col, Object expected) {
		assertThat(table.getModel().getValue(row, col))
				.as(String.format("Row %d, Column %d should be: %s", row, col, expected))
				.isEqualTo(expected);
	}
}

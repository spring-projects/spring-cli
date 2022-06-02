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
import static org.springframework.cli.testutil.TableAssertions.verifyTableValue;

@ExtendWith(SystemStubsExtension.class)
public class ConfigCommandsTests {
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
		verifyTableValue(table, 0, 0, "Command");
		verifyTableValue(table, 0, 1, "Sub Command");
		verifyTableValue(table, 0, 2, "Option Name/Values");

		configCommands.configSet("boot", "new", "package-name", "com.xkcd");
		table = configCommands.configList();
		//System.out.println(table.render(80));
		verifyTableValue(table, 1, 0, "boot");
		verifyTableValue(table, 1, 1, "new");
		verifyTableValue(table, 1, 2, "['package-name' = 'com.xkcd']");
		assertThat(table.getModel().getRowCount()).isEqualTo(2);

		configCommands.configUnSet("boot", "new", "package-name");
		table = configCommands.configList();
		System.out.println(table.render(80));
		assertThat(table.getModel().getColumnCount()).isEqualTo(3);
		assertThat(table.getModel().getRowCount()).isEqualTo(1);
	}

}

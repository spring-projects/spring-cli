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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.roles.RoleService;
import org.springframework.cli.support.CommandRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;
import org.springframework.cli.util.StubTerminalMessage;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cli.testutil.TableAssertions.verifyTableValue;

public class RoleCommandTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockBaseConfig.class);

	@Test
	void defaultRole(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			StubTerminalMessage terminalMessage = new StubTerminalMessage();
			RoleCommands roleCommands = new RoleCommands(terminalMessage, workingDir);

			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.build();
			commandRunner.run();

			String role = "";  // default role
			// create key-value pair foo=bar in the default role
			roleCommands.roleSet("foo", "bar", role);
			Path varsFilePath = workingDir.resolve(".spring").resolve("roles")
					.resolve("vars").resolve("vars.yml");
			assertRoleContents(roleCommands, role, varsFilePath);
			String expected = terminalMessage.getPrintMessages().get(0);
			assertThat(expected).isEqualTo("Key-value pair added to the default role");
			roleCommands.roleGet("foo", role);
			expected = terminalMessage.getPrintMessages().get(1);
			assertThat(expected).isEqualTo("bar");

		});
	}

	private static void assertRoleContents(RoleCommands roleCommands, String role, Path varsFilePath) {
		assertThat(varsFilePath).exists();

		RoleService roleService = roleCommands.getRoleService();

		Map<String, Object> vars = roleService.loadAsMap(role);
		assertThat(vars).containsEntry("foo", "bar");
	}

	@Test
	void qaRole(final @TempDir Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			StubTerminalMessage terminalMessage = new StubTerminalMessage();
			RoleCommands roleCommands = new RoleCommands(terminalMessage, workingDir);

			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.build();
			commandRunner.run();

			String role = "qa";  // qa role

			// Add Role
			roleCommands.roleAdd(role);
			Path varsFilePath = workingDir.resolve(".spring").resolve("roles")
					.resolve("vars").resolve("vars-qa.yml");

			assertThat(varsFilePath).exists();
			String expected = terminalMessage.getPrintMessages().get(0);
			assertThat(expected).isEqualTo("Role 'qa' created.");

			// try adding again
			roleCommands.roleAdd(role);
			expected = terminalMessage.getPrintMessages().get(1);
			assertThat(expected).isEqualTo("Role 'qa' already exists.");

			// Add key value
			roleCommands.roleSet("foo", "bar", role);
			expected = terminalMessage.getPrintMessages().get(2);
			assertThat(expected).isEqualTo("Key-value pair added to role 'qa'");
			assertRoleContents(roleCommands, role, varsFilePath);

			Table table = roleCommands.roleList();
			verifyTableValue(table, 1, 0, "qa");

			// remove role
			roleCommands.roleRemove(role);
			expected = terminalMessage.getPrintMessages().get(3);
			assertThat(expected).isEqualTo("Role 'qa' deleted.");
		});
	}
}
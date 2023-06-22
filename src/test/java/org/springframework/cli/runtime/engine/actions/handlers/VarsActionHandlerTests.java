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


package org.springframework.cli.runtime.engine.actions.handlers;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.roles.RoleService;
import org.springframework.cli.support.AbstractShellTests;
import org.springframework.cli.support.CommandRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class VarsActionHandlerTests extends AbstractShellTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockBaseConfig.class);

	@Test
	void testExpressionDefineVarWithHandlebars(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {


			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("vars")
					.executeCommand("vars/data-handlebars")
					.withArguments("name-to-use", "Mary") // TODO consider name collisions. ('name' or 'user-name') won't work
					.withArguments("age", "20") //TODO change value to type Object
					.withArguments("height-unit", "feet")
					.withArguments("height", "6")
					.withArguments("is-person", "true")
					.build();
			commandRunner.run();


			RoleService roleService = new RoleService(workingDir);
			Map<String, Object> map = roleService.loadAsMap("");
			assertThat(map)
					.containsEntry("name", "Mary")
					.containsEntry("age", 20)
					.containsEntry("height-in-feet", 6)
					.containsEntry("person", true);
		});
	}

	@Test
	void testTrueIfExpressionDefineVarWithData(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {


			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("vars")
					.executeCommand("vars/iftrue")
					.build();
			commandRunner.run();


			RoleService roleService = new RoleService(workingDir);
			Map<String, Object> map = roleService.loadAsMap("");
			assertThat(map)
					.containsEntry("name", "John")
					.containsEntry("age", 30)
					.containsEntry("person", true);

		});
	}

	@Test
	void testFalseIfExpressionDefineVarWithData(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {


			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("vars")
					.executeCommand("vars/iffalse")
					.build();
			commandRunner.run();


			RoleService roleService = new RoleService(workingDir);
			Map<String, Object> map = roleService.loadAsMap("");
			assertThat(map).isEmpty();

		});

	}

	@Test
	void testDefineVarWithData(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("vars")
					.executeCommand("vars/data")
					.build();
			commandRunner.run();

			RoleService roleService = new RoleService(workingDir);
			Map<String, Object> map = roleService.loadAsMap("");

			assertThat(map)
					.containsEntry("name", "John")
					.containsEntry("age", 30)
					.containsEntry("person", true);
		});

	}

	@Test
	@Disabled
	void testDefineVarWithExec(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {


			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("vars")
					.executeCommand("vars/define")
					.withTerminal(getTerminal())
					.build();
			commandRunner.run();

			Thread.sleep(4000);

			TestBuffer testBuffer = new TestBuffer().cr();
			write(testBuffer.getBytes());

			RoleService roleService = new RoleService();
			Map<String, Object> map = roleService.loadAsMap("");
			assertThat(map).containsKey("phone-number");
		});

	}
}

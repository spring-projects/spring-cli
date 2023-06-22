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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.roles.RoleService;
import org.springframework.cli.support.CommandRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;

import static org.assertj.core.api.Assertions.assertThat;

class IfExpressionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockBaseConfig.class);

	@Test
	void testIfExpressionWithRunFile(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("if")
					.executeCommand("run/define")
					.build();
			commandRunner.run();
		});

		RoleService roleService = new RoleService(workingDir);
		Map<String, Object> map = roleService.loadAsMap("");
		assertThat(map).containsEntry("name", "John is present in the output");
	}


	@Test
	void testIfExpressionsWithSpel(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("if")
					.executeCommand("vars/notdefined")
					.withArguments("name", "mark")
					.build();
			commandRunner.run();

			RoleService roleService = new RoleService(workingDir);
			Map<String, Object> map = roleService.loadAsMap("");
			assertThat(map).containsEntry("name", "mark");
		});
	}


	@Test
	void testIfExpressions(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {
			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("if")
					.executeCommand("vars/define")
					.build();
			commandRunner.run();

			RoleService roleService = new RoleService(workingDir);
			Map<String, Object> map = roleService.loadAsMap("");

			//map.forEach((key, value) -> System.out.println(key + " " + value));
			assertThat(map)
					.containsEntry("name", "John")
					.containsEntry("middle-name", "Sam")
					.doesNotContainKey("last-name");
		});

	}
}

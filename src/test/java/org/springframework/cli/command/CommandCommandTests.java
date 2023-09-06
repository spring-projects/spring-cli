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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;
import org.springframework.cli.runtime.command.AbstractCommandTests;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.support.CommandRunner;
import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.sbm.boot.autoconfigure.DiscoveryConfiguration;
import org.springframework.sbm.parsers.RewriteParserConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandCommandTests extends AbstractCommandTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockBaseConfig.class);

	@TempDir(cleanup = CleanupMode.NEVER)
	Path workingDir;

	@Test
	void testCommandNew() {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("rest-service");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		this.contextRunner.withUserConfiguration(MockUserConfig.class, RewriteParserConfig.class, DiscoveryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CommandCommands.class);
			CommandCommands commandCommands = context.getBean(CommandCommands.class);
			commandCommands.commandNew("hello", "world", workingDir.toString());
			assertThat(workingDir).exists().isDirectory();
			assertThat(workingDir.resolve("pom.xml")).exists();
			Path helloWorldPath = workingDir.resolve(".spring").resolve("commands")
					.resolve("hello").resolve("world");
			assertThat(helloWorldPath.resolve("command.yaml")).exists();
			assertThat(helloWorldPath.resolve("hello.yaml")).exists();

			Map<String, Object> model = new HashMap<>();
			model.put("greeting", "world");
			assertThat(workingDir.resolve("hello.txt")).doesNotExist();
			Map<String, ModelPopulator> beansOfType = context.getBeansOfType(ModelPopulator.class);
			runCommand("hello", "world", model, new ArrayList<>(beansOfType.values()), String.valueOf(workingDir));
			assertThat(workingDir.resolve("hello.txt")).exists();
			String platform = System.getProperty("os.name")  + ".";
			assertThat(workingDir.resolve("hello.txt")).content().isEqualTo("Hello world on " + platform);
		});
	}
}

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

import javax.swing.Spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cli.git.GitSourceRepositoryService;
import org.springframework.cli.support.AbstractShellTests;
import org.springframework.cli.support.SpringCliUserConfig;
import org.springframework.cli.util.IoUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cli.support.SpringCliUserConfig.SPRING_CLI_CONFIG_DIR;

@ExtendWith(SystemStubsExtension.class)
public class BootCommandsTests {

	@SystemStub
	private EnvironmentVariables environmentVariables;


	@Test
	void testDefaultNewProject(final @TempDir Path configDir, final @TempDir Path workingDir) {
		environmentVariables.set(IoUtils.TEST_WORKING_DIRECTORY, workingDir.toAbsolutePath().toString());
		environmentVariables.set(SPRING_CLI_CONFIG_DIR, configDir.toAbsolutePath().toString());
		SpringCliUserConfig springCliUserConfig = new SpringCliUserConfig();
		GitSourceRepositoryService gitSourceRepositoryService = new GitSourceRepositoryService(springCliUserConfig);
		BootCommands bootCommands = new BootCommands(springCliUserConfig, gitSourceRepositoryService);

		bootCommands.bootNew(null, null, null);
		assertThat(workingDir).exists().isDirectory();
		assertThat(workingDir.resolve("demo")).exists();

	}
}

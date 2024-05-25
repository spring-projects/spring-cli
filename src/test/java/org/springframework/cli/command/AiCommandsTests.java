/*
 * Copyright 2021-2024 the original author or authors.
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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.merger.ai.OpenAiHandler;
import org.springframework.cli.support.CommandRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;
import org.springframework.cli.util.TerminalMessage;

public class AiCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockBaseConfig.class);

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void addJpa(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			StubGenerateCodeAiService stubGenerateCodeAiService = new StubGenerateCodeAiService(TerminalMessage.noop());
			OpenAiHandler openAiHandler = new OpenAiHandler(stubGenerateCodeAiService);
			AiCommands aiCommands = new AiCommands(Optional.of(openAiHandler), TerminalMessage.noop());

			CommandRunner commandRunner = new CommandRunner.Builder(context).prepareProject("rest-service", workingDir)
				.build();
			commandRunner.run();

			// TODO this takes a while, need to move to integration test
			// aiCommands.aiAdd("jpa", workingDir.toAbsolutePath().toString(), true);

		});
	}

}

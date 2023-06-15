/*
 * Copyright 2021-2023 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.merger.ai.OpenAiHandler;
import org.springframework.cli.merger.ai.service.GenerateCodeAiService;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(command = "readme", group = "Readme")
public class ReadmeCommands {

	private final TerminalMessage terminalMessage;

	private final OpenAiHandler openAiHandler;

	@Autowired
	public ReadmeCommands(TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
		this.openAiHandler = new OpenAiHandler(new GenerateCodeAiService(this.terminalMessage));
	}

	public ReadmeCommands(OpenAiHandler openAiHandler, TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
		this.openAiHandler = openAiHandler;
	}

	@Command(command = "readme", description = "Apply the instructions in the readme to the code base.")
	public void readmeApply(
			@Option(description = "The readme file that contains the instructions of how to modify the code base, e.g. README-ai-jpa.md") String file,
			@Option(description = "Path to run the command in, most of the time this is not necessary to specify and the default value is the current working directory.") String path) {

		this.openAiHandler.apply(file, path, terminalMessage);
	}
}

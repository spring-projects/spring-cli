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

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.merger.ai.OpenAiHandler;
import org.springframework.cli.merger.ai.service.GenerateCodeAiService;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.standard.commands.Help;

@Command(command = "ai", group = "AI")
public class AiCommands implements ApplicationContextAware {

	private final TerminalMessage terminalMessage;

	private final OpenAiHandler openAiHandler;

	private ApplicationContext applicationContext;

	@Autowired
	public AiCommands(TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
		this.openAiHandler = new OpenAiHandler(new GenerateCodeAiService(this.terminalMessage));
	}

	public AiCommands(OpenAiHandler openAiHandler, TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
		this.openAiHandler = openAiHandler;
	}

	@Command(command = "add", description = "Add code to the project from AI for a Spring Project project.")
	public void aiAdd(
			@Option(description = "The description of the code to create, this can be as short as a well known Spring project name, e.g JPA.", required = true) String description,
			@Option(description = "Path to run the command in, most of the time this is not necessary to specify and the default value is the current working directory.") String path,
			@Option(description = "Create the README.md file, but do not apply the changes to the code base.") boolean preview,
			@Option(description = "Rewrite the 'description' option the README.md file, but do not apply the changes to the code base.") boolean rewrite) {
		this.openAiHandler.add(description, path, preview, rewrite, terminalMessage);
	}

	private void printMissingDescriptionMessage() {
		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.WHITE));
		sb.append("Error: Missing required argument: [description]");
		sb.append(System.lineSeparator());
		if (this.applicationContext != null) {
			try {
				Help help = this.applicationContext.getBean(Help.class);
				String[] command = new String[] { "ai add" };
				this.terminalMessage.print(sb.toAttributedString());
				this.terminalMessage.print(help.help(command));
			} catch (Exception ex) {
				// do nothing
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}

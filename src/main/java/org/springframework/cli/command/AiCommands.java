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

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import org.springframework.beans.BeansException;
import org.springframework.cli.merger.ai.OpenAiHandler;
import org.springframework.cli.merger.ai.ResponseModifier;
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

	public AiCommands(Optional<OpenAiHandler> openAiHandler, TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
		this.openAiHandler = openAiHandler.orElseGet(() -> {
			return new OpenAiHandler(new GenerateCodeAiService(this.terminalMessage));
		});
	}

	@Command(command = "add", description = "Add code to the project from AI for a Spring project.")
	public void aiAdd(@Option(
			description = "The description of the code to create. This can be as short as a well known Spring project name, such as 'JPA'.",
			required = true) String description,
			@Option(description = "Path on which to run the command. Most of the time, you can not specify the path and use the default value, which is the current working directory.") String path,
			@Option(description = "Create the README.md file but do not apply the changes to the code base.") boolean preview,
			@Option(description = "Rewrite the 'description' option of the README.md file but do not apply the changes to the code base.") boolean rewrite) {
		this.openAiHandler.add(description, path, preview, rewrite, terminalMessage);
	}

	@Command(command = "enhance-response",
			description = "Enhance the AI response for a Spring project, i.e. make response Boot 3 compliant if necessary etc.")
	public String aiModifyResponse(
			@Option(description = "README.md file path containing the response from the AI.") String file,
			@Option(description = "Path on which to run the command. Most of the time, you can not specify the path and use the default value, which is the current working directory.") String path)
			throws JsonProcessingException {
		ResponseModifier responseModifier = new ResponseModifier();
		return responseModifier.modify(file, path);
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
			}
			catch (Exception ex) {
				// do nothing
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}

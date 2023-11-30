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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.context.InteractionMode;
import org.springframework.util.StringUtils;

@Command(command = ".", group = "Special Commands")
public class SpecialCommands {

	private final TerminalMessage terminalMessage;

	@Autowired
	public SpecialCommands(TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
	}

	@Command(command = "!", description = "run OS-specific shell command.", interactionMode = InteractionMode.INTERACTIVE)
	public void dotExec(@Option(description = "command") String command) {
		// Hack to try and avoid having to use quotes in the CLI to surround the command
		String[] stringArray = StringUtils.commaDelimitedListToStringArray(command);
		String commandToUse = StringUtils.arrayToDelimitedString(stringArray, " ");
		String[] commands = getCommands(commandToUse);
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		processBuilder.inheritIO();
		try {
			processBuilder.start().waitFor();
		} catch (Exception ex) {
			throw new SpringCliException("Exception running OS command: " + command +
					" Error message: " + ex.getMessage(), ex);
		}
		this.terminalMessage.print("");
	}

	private String[] getCommands(String command) {
		String os = System.getProperty("os.name");
		if (os.toLowerCase().contains("win")) {
			return new String[]{"cmd", "/c", command};
		} else {
			return new String[]{"bash", "-c", command};
		}
	}
}

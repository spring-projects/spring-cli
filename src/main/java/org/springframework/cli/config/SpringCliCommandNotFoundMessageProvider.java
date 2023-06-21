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


package org.springframework.cli.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.shell.Utils;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.result.CommandNotFoundMessageProvider;
import org.springframework.shell.standard.commands.Help;

public class SpringCliCommandNotFoundMessageProvider implements CommandNotFoundMessageProvider, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public String apply(ProviderContext providerContext) {
		String commands =  String.join(" ", providerContext.commands());
		Help help = this.applicationContext.getBean(Help.class);
		CommandCatalog commandCatalog = this.applicationContext.getBean(CommandCatalog.class);
		Optional<String> group = isLikePartialCommand(commands, commandCatalog);
		if (group.isPresent()) {
			try {
				return commandHelpMessage(commands, commandCatalog, providerContext, group.get(), help);
			}
			catch (IOException e) {
				return defaultHelpMessage(providerContext);
			}
		}
		if (isLikeHelp(commands)) {
			try {
				AttributedStringBuilder sb = new AttributedStringBuilder();
				String message = new AttributedString(providerContext.error().getMessage(),
						AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).toAnsi();
				sb.append(message);
				sb.append(System.lineSeparator());
				sb.append(help.help(null));
				return sb.toAttributedString().toAnsi();
			}
			catch (IOException e) {
				return defaultHelpMessage(providerContext);
			}
		}

		return defaultHelpMessage(providerContext);
	}

	private String commandHelpMessage(String commands, CommandCatalog commandCatalog, ProviderContext providerContext, String matchedGroupName, Help help) throws IOException {
		StringBuilder result = new StringBuilder();
		result.append(defaultHelpMessage(providerContext));
		result.append("\n");
		String defaultMessage = help.help(null).toAnsi();
		String [] lines = defaultMessage.split("\n");
		for (String ansiLine : lines) {
			String line = AttributedString.stripAnsi(ansiLine);
			if (line.trim().split("\\s+")[0].equalsIgnoreCase(matchedGroupName)) {
				if (line.toLowerCase().contains(matchedGroupName.toLowerCase())) {
					result.append(ansiLine).append("\n");
				}
			}
		}
		return result.toString();
	}

	private Optional<String> isLikePartialCommand(String commands, CommandCatalog commandCatalog) {
		String possibleGroupName = commands.split("\\s+")[0];
		Map<String, CommandRegistration> registrations = Utils
				.removeHiddenCommands(commandCatalog.getRegistrations());
		for (CommandRegistration commandRegistration : registrations.values()) {
			if (possibleGroupName.equalsIgnoreCase(commandRegistration.getGroup())) {
				return Optional.of(commandRegistration.getGroup());
			}
		}
		return Optional.empty();
	}

	private static String defaultHelpMessage(ProviderContext providerContext) {
		return new AttributedString(providerContext.error().getMessage()
				+ "  Use 'spring help' to get help.",
				AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).toAnsi();
	}

	private boolean isLikeHelp(String command) {
		List<String> wordList = Arrays.asList(
				"-h", "-help",
				"--h", "--help",
				 "-hlep",  "-hep", "-hel",   "-hlpe", "-elp",
				"--hlep", "--hep", "--hel", "--hlep", "--elp",
				"hlep", "hep", "hel", "hlpe");
		return wordList.stream().anyMatch(command::contains);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.util.FileSystemUtils;

/**
 * Commands related to managing user defined commands.
 *
 * User defined commands can be added and removed using `command add` and `command delete`
 *
 * A simple command that creates a directory structure, aciton file, and command
 * metadata file is available using `command new`
 */
@Command(command = "command", group = "Command")
public class CommandCommands extends AbstractSpringCliCommands  {

	private static final Logger logger = LoggerFactory.getLogger(CommandCommands.class);

	private final SourceRepositoryService sourceRepositoryService;

	private final TerminalMessage terminalMessage;

	@Autowired
	public CommandCommands(SourceRepositoryService sourceRepositoryService, TerminalMessage terminalMessage) {
		this.sourceRepositoryService = sourceRepositoryService;
		this.terminalMessage = terminalMessage;
	}

	@Command(command = "new", description="Create a new user-defined command")
	public void commandNew(
			@Option(description = "The name of the user-defined command to create", defaultValue = "hello") String commandName,
			@Option(description = "The name of the user-defined sub-command to create", defaultValue = "new") String subCommandName,
			@Option(description = "Path to execute command in") String path) {
		Path projectPath = path != null ? IoUtils.getProjectPath(path) : IoUtils.getWorkingDirectory();
		//TODO check validity of passed in names as directory names.
		Path commandPath = projectPath.resolve(".spring").resolve("commands").resolve(commandName).resolve(subCommandName);
		IoUtils.createDirectory(commandPath);
		ClassPathResource classPathResource = new ClassPathResource("/org/springframework/cli/commands/hello.yml");
		IoUtils.writeToDir(commandPath.toFile(), "hello.yaml", classPathResource);
		classPathResource = new ClassPathResource("org/springframework/cli/commands/command.yaml");
		IoUtils.writeToDir(commandPath.toFile(), "command.yaml", classPathResource);
		terminalMessage.print("Created user defined command " + commandPath);

	}

	@Command(command = "add", description = "Add a user-defined command")
	public void commandAdd(
			@Option(description = "Add user-defined command from a URL") String from) {
		Path downloadedCommandPath = sourceRepositoryService.retrieveRepositoryContents(from);
		logger.debug("downloaded command path ", downloadedCommandPath);
		Path cwd = IoUtils.getWorkingDirectory().toAbsolutePath();

		try {
			FileSystemUtils.copyRecursively(downloadedCommandPath, cwd);
		}
		catch (IOException e) {
			throw new SpringCliException("Could not add command", e);
		}

		// Display which commands were added.
		Path commandsPath = Paths.get(downloadedCommandPath.toString(), ".spring", "commands");

		if (Files.exists(commandsPath)) {
			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.WHITE));
			File[] files = commandsPath.toFile().listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					sb.append("Command " + file.getName() + " added.");
					sb.append(System.lineSeparator());
				}
			}

			for (File file : files) {
				String readmeName = "README-" + file.getName() + ".md";
				Path readmePath = Paths.get(cwd.toString(), readmeName);
				logger.debug("README PATH = "+ readmePath);
				if (Files.exists(readmePath)) {
					sb.append("Refer to " + readmeName + " for more information.");
					sb.append(System.lineSeparator());
				}
			}
			sb.append("Execute 'spring help' for more information on User-defined commands.");
			terminalMessage.print(sb.toAttributedString());

			try {
				FileSystemUtils.deleteRecursively(downloadedCommandPath);
			} catch (IOException ex) {
				logger.warn("Could not delete path " + downloadedCommandPath, ex);
			}
		}
	}

	@Command(command = "remove", description = "Delete a user-defined command")
	public void commandDelete(
			@Option(description = "Command name") String commandName,
			@Option(description = "SubCommand name") String subCommandName) {
		Path cwd = IoUtils.getWorkingDirectory();
		Path dynamicSubCommandPath = Paths.get(cwd.toString(), ".spring", "commands")
				.resolve(commandName).resolve(subCommandName).toAbsolutePath();
		try {
			FileSystemUtils.deleteRecursively(dynamicSubCommandPath);
			logger.debug("Deleted " + dynamicSubCommandPath);
		}
		catch (IOException e) {
			throw new SpringCliException("Could not delete " + dynamicSubCommandPath, e);
		}
	}

}
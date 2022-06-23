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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.util.IoUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.FileSystemUtils;

@ShellComponent
public class CommandCommands {

	private static final Logger logger = LoggerFactory.getLogger(CommandCommands.class);

	private final SourceRepositoryService sourceRepositoryService;

	@Autowired
	public CommandCommands(SourceRepositoryService sourceRepositoryService) {
		this.sourceRepositoryService = sourceRepositoryService;
	}

	@ShellMethod(key = "command new", value = "Create a new user-defined command")
	public void commandNew(
			@ShellOption(help = "The name of the user-defined command to create", arity = 1, defaultValue = "hello") String commandName,
			@ShellOption(help = "The name of the user-defined sub-command to create", arity = 1, defaultValue = "new") String subCommandName,
			@ShellOption(help = "Path to execute command in", defaultValue = ShellOption.NULL, arity = 1) String path) {

		Path projectPath = path != null ? IoUtils.getProjectPath(path) : IoUtils.getWorkingDirectory();
		//TODO check validity of passed in names as directory names.
		Path commandPath = projectPath.resolve(".spring").resolve("commands").resolve(commandName).resolve(subCommandName);
		IoUtils.createDirectory(commandPath);
		ClassPathResource classPathResource = new ClassPathResource("/org/springframework/cli/commands/hello.yml");
		IoUtils.writeToDir(commandPath.toFile(), "hello.yml", classPathResource);
		classPathResource = new ClassPathResource("org/springframework/cli/commands/command.yaml");
		IoUtils.writeToDir(commandPath.toFile(), "command.yaml", classPathResource);
		System.out.println("Created user defined command " + commandPath);

	}

	@ShellMethod(key = "command add", value = "Add a user-defined command")
	public void commandAdd(
			@ShellOption(help = "Add user-defined command from a URL", arity = 1, defaultValue = "hello") String from) {
		Path downloadedCommandPath = sourceRepositoryService.retrieveRepositoryContents(from);
		Path cwd = IoUtils.getWorkingDirectory().toAbsolutePath();
		try {
			FileSystemUtils.copyRecursively(downloadedCommandPath, cwd);
			// TODO use terminal
			System.out.println("Added user-defined command.");
		}
		catch (IOException e) {
			throw new SpringCliException("Could not add command", e);
		}
	}

	@ShellMethod(key = "command remove", value = "Delete a user-defined command")
	public void commandDelete(
			@ShellOption(help = "Command name", arity = 1) String commandName,
			@ShellOption(help = "SubCommand name", arity = 1) String subCommandName) {
		Path cwd = IoUtils.getWorkingDirectory();
		Path dynamicSubCommandPath = Paths.get(cwd.toString(), ".spring", "commands")
				.resolve(commandName).resolve(subCommandName).toAbsolutePath();
		try {
			FileSystemUtils.deleteRecursively(dynamicSubCommandPath);
			System.out.println("Deleted " + dynamicSubCommandPath);
		}
		catch (IOException e) {
			throw new SpringCliException("Could not delete " + dynamicSubCommandPath, e);
		}
	}
}
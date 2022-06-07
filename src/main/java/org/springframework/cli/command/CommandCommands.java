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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.util.IoUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class CommandCommands {

	private static final Logger logger = LoggerFactory.getLogger(CommandCommands.class);

	@ShellMethod(key = "command new", value = "Create a new user-defined command")
	public void commandNew(
			@ShellOption(help = "The name of the user-defined command to create", arity = 1 , defaultValue = "hello") String commandName,
			@ShellOption(help = "The name of the user-defined sub-command to create", arity = 1, defaultValue = "new") String subCommandName,
			@ShellOption(help = "Path to execute command in", defaultValue = ShellOption.NULL, arity = 1) String path) {

		Path projectPath = path != null ? IoUtils.getProjectPath(path) : IoUtils.getWorkingDirectory();
		//TODO check validity of passed in names as directory names.
		Path commandPath = projectPath.resolve(".spring").resolve("commands").resolve(commandName).resolve(subCommandName);
		ClassPathResource classPathResource = new ClassPathResource("org/springframework/cli/commands/hello.yml");
		IoUtils.createDirectory(commandPath);
		IoUtils.writeToDir(commandPath.toFile(), "hello.txt", classPathResource);
		System.out.println("Created user defined command " + commandPath);


	}


}

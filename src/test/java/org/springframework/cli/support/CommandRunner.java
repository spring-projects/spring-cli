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


package org.springframework.cli.support;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.jline.terminal.Terminal;

import org.springframework.cli.runtime.command.DynamicCommand;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class CommandRunner {

	private final ApplicationContext context;
	private final String projectName;
	private final Path workingDir;

	private final String commandGroup;

	private final String executeCommand;
	private final List<Map.Entry<String, String>> arguments;

	private final Terminal terminal;

	private CommandRunner(Builder builder) {
		this.context = builder.context;
		this.projectName = builder.projectName;
		this.workingDir = builder.workingDir;
		this.commandGroup = builder.commandGroup;
		this.executeCommand = builder.executeCommand;
		this.arguments = builder.arguments;
		this.terminal = builder.terminal;
	}

	public void run() {

		if (executeCommand != null) {
			String[] commandAndSubCommand = executeCommand.split("\\/");
			Map<String, ModelPopulator> modelPopulatorMap = context.getBeansOfType(ModelPopulator.class);
			DynamicCommand dynamicCommand = new DynamicCommand(commandAndSubCommand[0], commandAndSubCommand[1],
					new ArrayList<>(modelPopulatorMap.values()), TerminalMessage.noop(), Optional.of(this.terminal));

			Map<String, Object> model = new HashMap<>();
			for (Entry<String, String> argument : arguments) {
				model.put(argument.getKey(), argument.getValue());
			}

			if (commandGroup != null) {
				Path commandPath = workingDir.resolve(".spring").resolve("commands")
						.resolve(commandAndSubCommand[0])
						.resolve(commandAndSubCommand[1]);
				assertThat(commandPath).withFailMessage("Directory for command "
						+ commandAndSubCommand + " could not be found.\n  Looked in directory "
						+ commandGroup +
						".\n  Check that 'installCommandGroup' and 'executeCommand' are not mismatched.\n");
				dynamicCommand.runCommand(workingDir, ".spring", "commands", model);
			}
		}
	}

	public static class Builder {
		// Optional parameters
		private ApplicationContext context;
		private String projectName;

		private Path projectPath;
		private Path workingDir;

		private String commandGroup;

		private Path commandGroupPath;

		private String executeCommand;

		private Terminal terminal = mock(Terminal.class);

		private List<Map.Entry<String, String>> arguments = new ArrayList<>();

		public Builder(ApplicationContext context) {
			this.context = Objects.requireNonNull(context);
		}

		public Builder prepareProject(String projectName, Path workingDir) {
			this.projectName = Objects.requireNonNull(projectName);
			this.workingDir = Objects.requireNonNull(workingDir);
			Path resolvedPath = Path.of("test-data").resolve("projects").resolve(projectName);
			Resource resource = new FileSystemResource(resolvedPath.toString());
			try {
				assertThat(resource.getFile()).exists();
				this.projectPath = Path.of(resource.getFile().getAbsolutePath());
			}
			catch (IOException e) {
				fail("Project name " + projectName + " could not resolved to the directory " + resource.getDescription());
			}
			return this;
		}

		public Builder installCommandGroup(String commandGroup) {
			this.commandGroup = Objects.requireNonNull(commandGroup);
			Path commandGroupPath = Path.of("test-data").resolve("commands").resolve(commandGroup);
			Resource resource = new FileSystemResource(commandGroupPath.toString());
			try {
				assertThat(resource.getFile()).exists();
				this.commandGroupPath = Path.of(resource.getFile().getAbsolutePath());
			}
			catch (IOException e) {
				fail("Command group name " + commandGroup + " could not resolved to the directory " + resource.getDescription());
			}
			return this;
		}

		public Builder executeCommand(String command) {
			this.executeCommand = Objects.requireNonNull(command);
			String[] commandAndSubCommand = command.split("\\/");
			assertThat(commandAndSubCommand)
					.withFailMessage("The command must be of the form 'command/subcommand'. Actual value = '" + command + "'")
					.hasSize(2);
			return this;
		}

		public Builder withTerminal(Terminal terminal) {
			this.terminal = Objects.requireNonNull(terminal);
			return this;
		}

		public Builder withArguments(String key, String value) {
			Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>(key, value);
			this.arguments.add(entry);
			return this;
		}

		public CommandRunner build() {
			this.projectName = Objects.requireNonNull(projectName);
			assertThat(projectName)
					.withFailMessage("Please invoke the method 'prepareProject' with the project name and temporary working directory")
					.isNotNull();
			IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);

			if (commandGroupPath != null) {
				Path commandInstallPath = workingDir.resolve(".spring").resolve("commands");
				IntegrationTestSupport.installInWorkingDirectory(commandGroupPath, commandInstallPath);
			}
			return new CommandRunner(this);
		}
	}
}


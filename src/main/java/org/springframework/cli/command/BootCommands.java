/*
 * Copyright 2021-2022 the original author or authors.
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

import org.jline.utils.AttributedString;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.merger.ProjectHandler;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class BootCommands extends AbstractSpringCliCommands {

	private static final String FALLBACK_DEFAULT_PACKAGE_NAME = "com.example";
	private final  SpringCliUserConfig springCliUserConfig;
	private final SourceRepositoryService sourceRepositoryService;
	private TerminalMessage terminalMessage = new DefaultTerminalMessage();

	@Autowired
	public BootCommands(SpringCliUserConfig springCliUserConfig,
			SourceRepositoryService sourceRepositoryService) {
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
	}

	@ShellMethod(key = "boot new", value = "Create a new Spring Boot project from an existing project")
	public void bootNew(
			@ShellOption(help = "Create project from existing project name or URL", defaultValue = ShellOption.NULL, arity = 1) String from,
			@ShellOption(help = "Name of the new project", defaultValue = ShellOption.NULL, arity = 1) String name,
			@ShellOption(help = "Package name for the new project", defaultValue = ShellOption.NULL, arity = 1, value="--package-name") String packageName,
			@ShellOption(help = "Path", defaultValue = ShellOption.NULL, arity = 1) String path) {
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.create(from, name, packageName, path);
	}

	@ShellMethod(key = "boot add", value = "Merge an existing project into the current Spring Boot project")
	public void bootAdd(
			@ShellOption(help = "Add to project from an existing project name or URL", arity = 1) String from,
			@ShellOption(help = "Path", defaultValue = ShellOption.NULL, arity = 1) String path) {
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.add(from, path);
	}

	public void setTerminalMessage(TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
	}

	private class DefaultTerminalMessage implements TerminalMessage {

		@Override
		public void shellPrint(String... text) {
			BootCommands.this.shellPrint(text);
		}

		@Override
		public void shellPrint(AttributedString... text) {
			BootCommands.this.shellPrint(text);
		}
	}
}

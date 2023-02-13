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
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.merger.ProjectHandler;
import org.springframework.cli.util.ProjectInfo;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class BootCommands extends AbstractSpringCliCommands {

	private final  SpringCliUserConfig springCliUserConfig;
	private final SourceRepositoryService sourceRepositoryService;

	private final TerminalMessage terminalMessage;

	@Autowired
	public BootCommands(SpringCliUserConfig springCliUserConfig,
			SourceRepositoryService sourceRepositoryService,
			TerminalMessage terminalMessage) {
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
		this.terminalMessage = terminalMessage;
	}

	@ShellMethod(key = "boot new", value = "Create a new Spring Boot project from an existing project")
	public void bootNew(
			@ShellOption(help = "Create project from existing project name or URL", defaultValue = ShellOption.NULL, arity = 1) String from,
			@ShellOption(help = "Name of the new project", defaultValue = ShellOption.NULL, arity = 1) String name,
			@ShellOption(help = "Group ID of the new project", defaultValue = ShellOption.NULL, arity = 1, value="--group-id") String groupId,
			@ShellOption(help = "Artifact ID of the new project", defaultValue = ShellOption.NULL, arity = 1, value="--artifact-id") String artifactId,
			@ShellOption(help = "Version of the new project", defaultValue = ShellOption.NULL, arity = 1) String version,
			@ShellOption(help = "Description of the new project", defaultValue = ShellOption.NULL, arity = 1) String description,
			@ShellOption(help = "Package name for the new project", defaultValue = ShellOption.NULL, arity = 1, value="--package-name") String packageName,
			@ShellOption(help = "Path to run the command in, most of the time this is not necessary to specify and the default value is the current working directory.", defaultValue = ShellOption.NULL, arity = 1) String path) {
		ProjectInfo projectInfo = new ProjectInfo(groupId, artifactId, version, name, description, packageName);
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.create(from, path, projectInfo.getDefaults());
	}


	@ShellMethod(key = "boot add", value = "Merge an existing project into the current Spring Boot project")
	public void bootAdd(
			@ShellOption(help = "Add to project from an existing project name or URL", arity = 1) String from,
			@ShellOption(help = "Path", defaultValue = ShellOption.NULL, arity = 1) String path) {
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.add(from, path);
	}

}

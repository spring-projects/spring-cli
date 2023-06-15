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
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(command = "boot", group = "Boot")
public class BootCommands extends AbstractSpringCliCommands {

	private final SpringCliUserConfig springCliUserConfig;
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

	@Command(command = "new", description = "Create a new Spring Boot project from an existing project")
	public void bootNew(
			@Option(description = "Create project from existing project name or URL") String from,
			@Option(description = "Name of the new project") String name,
			@Option(longNames = "group-id", description = "Group ID of the new project") String groupId,
			@Option(longNames = "artifact-id", description = "Artifact ID of the new project") String artifactId,
			@Option(description = "Version of the new project") String version,
			@Option(description = "Description of the new project") String description,
			@Option(longNames = "package-name", description = "Package name for the new project") String packageName,
			@Option(description = "Path to run the command in, most of the time this is not necessary to specify and the default value is the current working directory.") String path) {
		ProjectInfo projectInfo = new ProjectInfo(groupId, artifactId, version, name, description, packageName);
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.create(from, path, projectInfo.getDefaults());
	}


	@Command(command = "add", description = "Merge an existing project into the current Spring Boot project")
	public void bootAdd(
			@Option(description = "Add to project from an existing project name or URL") String from,
			@Option(description = "Path") String path) {
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.add(from, path);
	}

}

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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepositories;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepository;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.support.configfile.YamlConfigFile;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ObjectUtils;

@Command(command = "project", group = "Project")
public class ProjectCommands {

	private static final Logger logger = LoggerFactory.getLogger(ProjectCommands.class);

	private final SpringCliUserConfig upCliUserConfig;

	private final SourceRepositoryService sourceRepositoryService;

	private final TerminalMessage terminalMessage;
	private final ObjectMapper objectMapper;

	@Autowired
	public ProjectCommands(SpringCliUserConfig upCliUserConfig,
						   SourceRepositoryService sourceRepositoryService,
						   TerminalMessage terminalMessage, ObjectMapper objectMapper) {
		this.upCliUserConfig = upCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
		this.terminalMessage = terminalMessage;
		this.objectMapper = objectMapper;
	}

	@Command(command = "add", description = "Add a project to use with the 'boot new' and 'boot add' commands")
	public void projectAdd(
		@Option(description = "Project name", required = true) String name,
		@Option(description = "Project url", required = true) String url,
		@Option(description = "Project description") String description,
		@Option(description = "Project tags") List<String> tags
	) {
		List<ProjectRepository> projectRepositories = upCliUserConfig.getProjectRepositories().getProjectRepositories();
		projectRepositories.add(ProjectRepository.of(name, description, url, tags));
		ProjectRepositories projectRepositoriesConfig = new ProjectRepositories();
		projectRepositoriesConfig.setProjectRepositories(projectRepositories);
		upCliUserConfig.setProjectRepositories(projectRepositoriesConfig);
		this.terminalMessage.print("Project '" + name + "' added from URL = " + url);
	}

	private record ProjectRepositoryData(String name, String url, String description, List<String> tags, String catalog) {}

	@Command(command = "list", description = "List projects available for use with the 'boot new' and 'boot add' commands")
	public Object projectList(
			@Option(description = "JSON format output", required = false, defaultValue = "false") boolean json
	) throws JsonProcessingException {
		// Retrieve project that were registered using the `project add` command and stored locally
		List<ProjectRepository> repos = upCliUserConfig.getProjectRepositories().getProjectRepositories();
		List<ProjectRepositoryData> projectRepositories = (repos == null ? Stream.<ProjectRepository>empty() : repos.stream())
				.map(pr -> new ProjectRepositoryData(pr.getName(), pr.getUrl(), pr.getDescription(), pr.getTags(), null))
				.collect(Collectors.toList());

		// List projects that are contained in catalogs that the user had added using the `project-catalog add` command
		List<ProjectCatalog> projectCatalogs = upCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		for (ProjectCatalog projectCatalog : projectCatalogs) {
			String url = projectCatalog.getUrl();
			Path path = sourceRepositoryService.retrieveRepositoryContents(url);
			YamlConfigFile yamlConfigFile = new YamlConfigFile();
			for (ProjectRepository pr : yamlConfigFile.read(Paths.get(path.toString(),"project-catalog.yml"), ProjectRepositories.class).getProjectRepositories()) {
				projectRepositories.add(new ProjectRepositoryData(pr.getName(), pr.getUrl(), pr.getDescription(), pr.getTags(), projectCatalog.getName()));
			}
			// clean up temp files
			try {
				FileSystemUtils.deleteRecursively(path);
			} catch (IOException ex) {
				logger.warn("Could not delete path " + path, ex);
			}
		}

		if (json) {
			return objectMapper.writeValueAsString(projectRepositories);
		}

		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description",  "URL", "Catalog", "Tags" });

		// Retrieve project that were registered using the `project add` command and stored locally
		Stream<String[]> rows = projectRepositories.stream()
			.map(tr -> new String[] { tr.name(),
					Objects.requireNonNullElse(tr.description, ""),
					tr.url(),
					tr.catalog,
					(Objects.requireNonNullElse(tr.tags(), "")).toString() }
		);

		String[][] data = Stream.concat(header, rows).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	@Command(command = "remove", description = "Remove project")
	public void projectRemove(
		@Option(description = "Project name", required = true) String name
	) {
		List<ProjectRepository> origininalProjectRepositories = upCliUserConfig.getProjectRepositories().getProjectRepositories();
		List<ProjectRepository> updatedProjectRepositories = origininalProjectRepositories.stream()
			.filter(tc -> !ObjectUtils.nullSafeEquals(tc.getName(), name))
			.collect(Collectors.toList());
		if (updatedProjectRepositories.size() < origininalProjectRepositories.size()) {
			ProjectRepositories projectRepositoriesConfig = new ProjectRepositories();
			projectRepositoriesConfig.setProjectRepositories(updatedProjectRepositories);
			upCliUserConfig.setProjectRepositories(projectRepositoriesConfig);
			this.terminalMessage.print("Project '" + name + "' removed");
		} else {
			this.terminalMessage.print("Project '" + name + "' removed");
		}
	}

}

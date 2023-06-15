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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepositories;
import org.springframework.cli.config.SpringCliUserConfig.ProjectRepository;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.support.configfile.YamlConfigFile;
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

	@Autowired
	public ProjectCommands(SpringCliUserConfig upCliUserConfig, SourceRepositoryService sourceRepositoryService) {
		this.upCliUserConfig = upCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
	}

	@Command(command = "add", description = "Add a project to use with 'boot new' and 'boot add' commands")
	public void projectAdd(
		@Option(description = "Project name") String name,
		@Option(description = "Project url") String url,
		@Option(description = "Project description") String description,
		@Option(description = "Project tags") List<String> tags
	) {
		List<ProjectRepository> projectRepositories = upCliUserConfig.getProjectRepositories().getProjectRepositories();
		projectRepositories.add(ProjectRepository.of(name, description, url, tags));
		ProjectRepositories projectRepositoriesConfig = new ProjectRepositories();
		projectRepositoriesConfig.setProjectRepositories(projectRepositories);
		upCliUserConfig.setProjectRepositories(projectRepositoriesConfig);
	}

	@Command(command = "list", description = "List projects available for use with 'boot new' and 'boot add' commands")
	public Table projectList() {

		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description",  "URL", "Catalog", "Tags" });
		Collection<ProjectRepository> projectRepositories = upCliUserConfig.getProjectRepositories().getProjectRepositories();
		Stream<String[]> rows;
		if (projectRepositories != null) {
			rows = projectRepositories.stream()
				.map(tr -> new String[] { tr.getName(), tr.getUrl(),
						Objects.requireNonNullElse(tr.getDescription(), ""),
						"",
						(Objects.requireNonNullElse(tr.getTags(), "")).toString() }
			);
		}
		else {
			rows = Stream.empty();
		}
		List<String[]> allRows = rows.collect(Collectors.toList());

		List<ProjectCatalog> projectCatalogs = upCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		for (ProjectCatalog projectCatalog : projectCatalogs) {
			String url = projectCatalog.getUrl();
			Path path = sourceRepositoryService.retrieveRepositoryContents(url);
			YamlConfigFile yamlConfigFile = new YamlConfigFile();
			projectRepositories = yamlConfigFile.read(Paths.get(path.toString(),"project-repositories.yml"),
					ProjectRepositories.class).getProjectRepositories();
			Stream<String[]> fromCatalogRows;
			if (projectRepositories != null) {
				fromCatalogRows = projectRepositories.stream()
						.map(tr -> new String[] {
								tr.getName(),
								Objects.requireNonNullElse(tr.getDescription(), ""),
								tr.getUrl(),
								projectCatalog.getName(),
								(Objects.requireNonNullElse(tr.getTags(), "")).toString() }
						);
			} else {
				fromCatalogRows = Stream.empty();
			}
			List<String[]> projectCatalogRows = fromCatalogRows.collect(Collectors.toList());
			allRows.addAll(projectCatalogRows);

			// clean up temp files
			try {
				FileSystemUtils.deleteRecursively(path);
			} catch (IOException ex) {
				logger.warn("Could not delete path " + path, ex);
			}

		}

		String[][] data = Stream.concat(header, allRows.stream()).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	@Command(command = "remove", description = "Remove project")
	public void projectRemove(
		@Option(defaultValue = "Project name") String name
	) {
		List<ProjectRepository> projectRepositories = upCliUserConfig.getProjectRepositories().getProjectRepositories();
		projectRepositories = projectRepositories.stream()
			.filter(tc -> !ObjectUtils.nullSafeEquals(tc.getName(), name))
			.collect(Collectors.toList());
		ProjectRepositories projectRepositoriesConfig = new ProjectRepositories();
		projectRepositoriesConfig.setProjectRepositories(projectRepositories);
		upCliUserConfig.setProjectRepositories(projectRepositoriesConfig);
	}
}

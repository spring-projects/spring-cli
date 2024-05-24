/*
 * Copyright 2022-2024 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.CatalogRepositories;
import org.springframework.cli.config.SpringCliUserConfig.CatalogRepository;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalogs;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.support.configfile.YamlConfigFile;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Command(command = "project-catalog", group = "Project Catalog")
public class ProjectCatalogCommands extends AbstractSpringCliCommands {

	private static final Logger logger = LoggerFactory.getLogger(ProjectCatalogCommands.class);

	private static final String PROJECT_CATALOG = "Project Catalog '";

	private final SpringCliUserConfig springCliUserConfig;

	private final SourceRepositoryService sourceRepositoryService;

	private final TerminalMessage terminalMessage;

	private final ObjectMapper objectMapper;

	@Autowired
	public ProjectCatalogCommands(SpringCliUserConfig springCliUserConfig,
			SourceRepositoryService sourceRepositoryService, TerminalMessage terminalMessage,
			ObjectMapper objectMapper) {
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
		this.terminalMessage = terminalMessage;
		this.objectMapper = objectMapper;
	}

	@Command(command = "list-available", description = "List available catalogs")
	public Object catalogListAvailable(
			@Option(description = "JSON format output", required = false, defaultValue = "false") boolean json)
			throws JsonProcessingException {
		Collection<CatalogRepository> catalogRepositories = getCatalogRepositories();
		if (json) {
			return objectMapper.writeValueAsString(catalogRepositories);
		}

		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description", "URL", "Tags" });
		List<String[]> allRows = new ArrayList<>();

		// TODO enforce all entries have name and Url
		Stream<String[]> fromCatalogRows;
		if (catalogRepositories != null) {
			fromCatalogRows = catalogRepositories.stream()
				.map(tr -> new String[] { tr.getName(), Objects.requireNonNullElse(tr.getDescription(), ""),
						tr.getUrl(), (Objects.requireNonNullElse(tr.getTags(), "")).toString() });
		}
		else {
			fromCatalogRows = Stream.empty();
		}
		List<String[]> catalogRepositoryRows = fromCatalogRows.toList();
		allRows.addAll(catalogRepositoryRows);

		String[][] data = Stream.concat(header, allRows.stream()).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	private Collection<CatalogRepository> getCatalogRepositories() {
		Path path = sourceRepositoryService
			.retrieveRepositoryContents("https://github.com/rd-1-2022/available-catalog-repositories");
		YamlConfigFile yamlConfigFile = new YamlConfigFile();
		Collection<CatalogRepository> catalogRepositories = yamlConfigFile
			.read(Paths.get(path.toString(), "catalog-repositories.yml"), CatalogRepositories.class)
			.getCatalogRepositories();

		// clean up temp files
		try {
			FileSystemUtils.deleteRecursively(path);
		}
		catch (IOException ex) {
			logger.warn("Could not delete path " + path, ex);
		}
		return catalogRepositories;
	}

	@Command(command = "list", description = "List installed catalogs")
	public Object catalogList(
			@Option(description = "JSON format output", required = false, defaultValue = "false") boolean json)
			throws JsonProcessingException {
		Collection<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		if (json) {
			return objectMapper.writeValueAsString(projectCatalogs);
		}
		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description", "URL", "Tags" });
		Stream<String[]> rows = null;
		if (projectCatalogs != null) {
			rows = projectCatalogs.stream()
				.map(tr -> new String[] { tr.getName(), Objects.requireNonNullElse(tr.getDescription(), ""),
						tr.getUrl(), (Objects.requireNonNullElse(tr.getTags(), "")).toString() });
		}
		else {
			rows = Stream.empty();
		}
		String[][] data = Stream.concat(header, rows).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	@Command(command = "add", description = "Add a project catalog")
	public void catalogAdd(@Option(description = "Catalog name", required = true) String name,
			@Option(description = "Catalog url", required = true) String url,
			@Option(description = "Catalog description") String description,
			@Option(description = "Project tags") List<String> tags) {
		if (url == null) {
			// look for name in available catalogs
			Collection<CatalogRepository> catalogRepositories = getCatalogRepositories();
			Optional<CatalogRepository> optionalCatalogRepository = catalogRepositories.stream()
				.filter(t -> ObjectUtils.nullSafeEquals(t.getName(), name))
				.findFirst();
			if (optionalCatalogRepository.isPresent()) {
				CatalogRepository catalogRepository = optionalCatalogRepository.get();
				// Currently installed project catalogs
				List<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
				checkIfCatalogNameExists(catalogRepository.getName(), projectCatalogs);
				// Add to currently installed project catalogs
				projectCatalogs.add(ProjectCatalog.of(catalogRepository.getName(), catalogRepository.getDescription(),
						catalogRepository.getUrl(), catalogRepository.getTags()));
				ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
				projectCatalogsConfig.setProjectCatalogs(projectCatalogs);
				springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
				this.terminalMessage.print(PROJECT_CATALOG + name + "' added.");
			}
			else {
				this.terminalMessage.print(PROJECT_CATALOG + name + "' not found.");
			}
		}
		else {
			// Add using URL
			List<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
			checkIfCatalogNameExists(name, projectCatalogs);
			projectCatalogs.add(ProjectCatalog.of(name, description, url, tags));
			ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
			projectCatalogsConfig.setProjectCatalogs(projectCatalogs);
			springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
			this.terminalMessage.print(PROJECT_CATALOG + name + "' added from URL = " + url);
		}
	}

	@Command(command = "remove", description = "Remove a project catalog")
	public void catalogRemove(@Option(description = "Catalog name", required = true) String name) {
		List<ProjectCatalog> originalProjectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		List<ProjectCatalog> updatedProjectCatalogs = originalProjectCatalogs.stream()
			.filter(tc -> !ObjectUtils.nullSafeEquals(tc.getName(), name))
			.toList();
		if (updatedProjectCatalogs.size() < originalProjectCatalogs.size()) {
			ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
			projectCatalogsConfig.setProjectCatalogs(updatedProjectCatalogs);
			springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
			this.terminalMessage.print("Catalog '" + name + "' removed");
		}
		else {
			this.terminalMessage.print("Catalog '" + name + "' not found");
		}

	}

	private void checkIfCatalogNameExists(String catalogName, List<ProjectCatalog> projectCatalogs) {
		for (ProjectCatalog projectCatalog : projectCatalogs) {
			if (StringUtils.hasText(catalogName) && projectCatalog.getName().equalsIgnoreCase(catalogName)) {
				throw new SpringCliException("Catalog named " + catalogName + " already exists.  Choose another name.");
			}
		}
	}

}

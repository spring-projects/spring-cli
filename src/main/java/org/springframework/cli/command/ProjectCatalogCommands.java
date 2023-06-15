/*
 * Copyright 2022-2023 the original author or authors.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Command(command = "catalog")
public class ProjectCatalogCommands extends AbstractSpringCliCommands {

	private static final Logger logger = LoggerFactory.getLogger(ProjectCatalogCommands.class);

	private final SpringCliUserConfig springCliUserConfig;

	private final SourceRepositoryService sourceRepositoryService;


	@Autowired
	public ProjectCatalogCommands(SpringCliUserConfig springCliUserConfig,
			SourceRepositoryService sourceRepositoryService) {
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
	}

	@Command(command = "list-available", description = "List available catalogs")
	public Table catalogListAvailable() {
		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description", "URL", "Tags"});
		List<String[]> allRows = new ArrayList<>();
		Collection<CatalogRepository> catalogRepositories = getCatalogRepositories();

		// TODO enforce all entries have name and Url
		Stream<String[]> fromCatalogRows;
		if (catalogRepositories != null) {
			fromCatalogRows = catalogRepositories.stream()
					.map(tr -> new String[] {
							tr.getName(),
							Objects.requireNonNullElse(tr.getDescription(), ""),
							tr.getUrl(),
							(Objects.requireNonNullElse(tr.getTags(), "")).toString()
					});
		} else {
			fromCatalogRows = Stream.empty();
		}
		List<String[]> catalogRepositoryRows = fromCatalogRows.collect(Collectors.toList());
		allRows.addAll(catalogRepositoryRows);

		String[][] data = Stream.concat(header, allRows.stream()).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	private Collection<CatalogRepository> getCatalogRepositories() {
		Path path = sourceRepositoryService.retrieveRepositoryContents("https://github.com/rd-1-2022/available-catalog-repositories");
		YamlConfigFile yamlConfigFile = new YamlConfigFile();
		Collection<CatalogRepository> catalogRepositories =
				yamlConfigFile.read(Paths.get(path.toString(),"catalog-repositories.yml"),
				CatalogRepositories.class).getCatalogRepositories();

		// clean up temp files
		try {
			FileSystemUtils.deleteRecursively(path);
		} catch (IOException ex) {
			logger.warn("Could not delete path " + path, ex);
		}
		return catalogRepositories;
	}


	@Command(command = "list", description = "List installed catalogs")
	public Table catalogList() {
		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description" , "URL", "Tags" });
		Collection<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		Stream<String[]> rows = null;
		if (projectCatalogs != null) {
			rows = projectCatalogs.stream()
				.map(tr -> new String[] {
						tr.getName(),
						Objects.requireNonNullElse(tr.getDescription(), ""),
						tr.getUrl(),
						(Objects.requireNonNullElse(tr.getTags(), "")).toString()
				});
		}
		else {
			rows = Stream.empty();
		}
		String[][] data = Stream.concat(header, rows).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	@Command(command = "add", description = "Add a project to a project catalog")
	public void catalogAdd(
			@Option(description = "Catalog name") String name,
			@Option(description = "Catalog url") String url,
			@Option(description = "Catalog description") String description,
			@Option(description = "Project tags") List<String> tags
	) {
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
				projectCatalogs.add(ProjectCatalog.of(
						catalogRepository.getName(),
						catalogRepository.getDescription(),
						catalogRepository.getUrl(),
						catalogRepository.getTags()));
				ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
				projectCatalogsConfig.setProjectCatalogs(projectCatalogs);
				springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
			}
		} else {
			List<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
			checkIfCatalogNameExists(name, projectCatalogs);
			projectCatalogs.add(ProjectCatalog.of(name, description, url, tags));
			ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
			projectCatalogsConfig.setProjectCatalogs(projectCatalogs);
			springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
		}
	}

	@Command(command = "remove", description = "Remove a project from a catalog")
	public void catalogRemove(
		@Option(description = "Catalog name") String name
	) {
		List<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		projectCatalogs = projectCatalogs.stream()
			.filter(tc -> !ObjectUtils.nullSafeEquals(tc.getName(), name))
			.collect(Collectors.toList());
		ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
		projectCatalogsConfig.setProjectCatalogs(projectCatalogs);
		springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
	}

	private void checkIfCatalogNameExists(String catalogName, List<ProjectCatalog> projectCatalogs) {
		for (ProjectCatalog projectCatalog : projectCatalogs) {
			if (StringUtils.hasText(catalogName) && projectCatalog.getName().equalsIgnoreCase(catalogName)) {
				throw new SpringCliException(
						"Catalog named " + catalogName + " already exists.  Choose another name.");
			}
		}
	}

}

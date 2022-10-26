/*
 * Copyright 2022 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalogs;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@ShellComponent
public class ProjectCatalogCommands extends AbstractSpringCliCommands {

	private final SpringCliUserConfig springCliUserConfig;

	@Autowired
	public ProjectCatalogCommands(SpringCliUserConfig springCliUserConfig) {
		this.springCliUserConfig = springCliUserConfig;
	}

	@ShellMethod(key = "catalog list", value = "List catalogs")
	public Table catalogList() {
		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "URL", "Description" , "Tags" });
		Collection<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		Stream<String[]> rows = null;
		if (projectCatalogs != null) {
			rows = projectCatalogs.stream()
				.map(tr -> new String[] {
						tr.getName(),
						tr.getUrl(),
						Objects.requireNonNullElse(tr.getDescription(), ""),
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

	@ShellMethod(key = "catalog add", value = "Add a project to a project catalog")
	public void catalogAdd(
			@ShellOption(help = "Catalog name", arity = 1) String name,
			@ShellOption(help = "Catalog url", arity = 1) String url,
			@ShellOption(help = "Catalog description", defaultValue = ShellOption.NULL, arity = 1) String description,
			@ShellOption(help = "Project tags", defaultValue = ShellOption.NULL, arity = 1) List<String> tags
	) {
		List<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		checkIfCatalogNameExists(name, projectCatalogs);
		projectCatalogs.add(ProjectCatalog.of(name, description, url, tags));
		ProjectCatalogs projectCatalogsConfig = new ProjectCatalogs();
		projectCatalogsConfig.setProjectCatalogs(projectCatalogs);
		springCliUserConfig.setProjectCatalogs(projectCatalogsConfig);
	}

	@ShellMethod(key = "catalog remove", value = "Remove a project from a catalog")
	public void catalogRemove(
		@ShellOption(help = "Catalog name", arity = 1) String name
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

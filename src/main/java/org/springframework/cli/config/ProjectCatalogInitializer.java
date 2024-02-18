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

package org.springframework.cli.config;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.config.SpringCliUserConfig.ProjectCatalogs;

/**
 * Registers a project catalog if there are no project catalogs already registered.
 */
public class ProjectCatalogInitializer implements InitializingBean {

	private SpringCliProjectCatalogProperties springCliProjectCatalogProperties;

	private SpringCliUserConfig springCliUserConfig;

	public ProjectCatalogInitializer(SpringCliUserConfig springCliUserConfig,
			SpringCliProjectCatalogProperties springCliProjectCatalogProperties) {
		this.springCliUserConfig = springCliUserConfig;
		this.springCliProjectCatalogProperties = springCliProjectCatalogProperties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<ProjectCatalog> projectCatalogList = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		if (projectCatalogList.isEmpty()) {
			// add to catalogs
			projectCatalogList.add(ProjectCatalog.of(this.springCliProjectCatalogProperties.getName(),
					this.springCliProjectCatalogProperties.getDescription(),
					this.springCliProjectCatalogProperties.getUrl(), this.springCliProjectCatalogProperties.getTags()));

			// Save to disk
			ProjectCatalogs projectCatalogs = new ProjectCatalogs();
			projectCatalogs.setProjectCatalogs(projectCatalogList);
			springCliUserConfig.setProjectCatalogs(projectCatalogs);
		}
	}

}

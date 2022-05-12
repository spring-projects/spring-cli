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
package org.springframework.up.support;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UpCliUserConfigTests {

	private FileSystem fileSystem;
	private Function<String, Path> pathProvider;

	@BeforeEach
	public void setupTests() {
		fileSystem = Jimfs.newFileSystem();
		pathProvider = (path) -> fileSystem.getPath(path);
	}

	@Test
	public void testHosts() {
		UpCliUserConfig config = new UpCliUserConfig(pathProvider);

		assertThat(config.getHosts()).isNull();

		UpCliUserConfig.Hosts hosts = new UpCliUserConfig.Hosts();
		Map<String, UpCliUserConfig.Host> hostsMap = new HashMap<>();
		hostsMap.put("github.com", new UpCliUserConfig.Host("faketoken", "user"));
		hosts.setHosts(hostsMap);
		config.setHosts(hosts);
		assertThat(config.getHosts()).isNotNull();
		assertThat(config.getHosts().get("github.com")).isNotNull();
		assertThat(config.getHosts().get("github.com").getOauthToken()).isEqualTo("faketoken");
	}

	@Test
	public void testTemplateCatalogs() {
		UpCliUserConfig config = new UpCliUserConfig(pathProvider);

		assertThat(config.getTemplateCatalogsConfig()).isNotNull();

		UpCliUserConfig.TemplateCatalogs catalogs = new UpCliUserConfig.TemplateCatalogs();
		List<UpCliUserConfig.TemplateCatalog> catalogList = new ArrayList<>();
		UpCliUserConfig.TemplateCatalog catalog1 = new UpCliUserConfig.TemplateCatalog("fakename1", "fakedesc1", "fakeurl1");
		UpCliUserConfig.TemplateCatalog catalog2 = new UpCliUserConfig.TemplateCatalog("fakename2", "fakedesc2", "fakeurl2");
		catalogList.add(catalog1);
		catalogList.add(catalog2);
		catalogs.setTemplateCatalogs(catalogList);
		config.setTemplateCatalogsConfig(catalogs);
		assertThat(config.getTemplateCatalogsConfig().getTemplateCatalogs()).hasSize(2);
	}

	@Test
	public void testTemplateRepositories() {
		UpCliUserConfig config = new UpCliUserConfig(pathProvider);

		assertThat(config.getTemplateRepositoriesConfig()).isNotNull();

		UpCliUserConfig.TemplateRepositories repositories = new UpCliUserConfig.TemplateRepositories();
		List<UpCliUserConfig.TemplateRepository> repositoryList = new ArrayList<>();
		UpCliUserConfig.TemplateRepository repository1 = new UpCliUserConfig.TemplateRepository("fakename1", "fakedesc1", "fakeurl1", new ArrayList<>());
		UpCliUserConfig.TemplateRepository repository2 = new UpCliUserConfig.TemplateRepository("fakename2", "fakedesc2", "fakeurl2", new ArrayList<>());
		repositoryList.add(repository1);
		repositoryList.add(repository2);
		repositories.setTemplateRepositories(repositoryList);
		config.setTemplateRepositoriesConfig(repositories);
		assertThat(config.getTemplateRepositoriesConfig().getTemplateRepositories()).hasSize(2);
	}
}

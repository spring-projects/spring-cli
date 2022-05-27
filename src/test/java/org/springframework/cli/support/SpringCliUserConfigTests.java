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
package org.springframework.cli.support;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cli.support.SpringCliUserConfig.CommandDefault;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCliUserConfigTests {

	private FileSystem fileSystem;
	private Function<String, Path> pathProvider;

	@BeforeEach
	public void setupTests() {
		fileSystem = Jimfs.newFileSystem();
		pathProvider = (path) -> fileSystem.getPath(path);
	}

	@Test
	public void testHosts() {
		SpringCliUserConfig config = new SpringCliUserConfig(pathProvider);

		assertThat(config.getHosts()).isNull();

		SpringCliUserConfig.Hosts hosts = new SpringCliUserConfig.Hosts();
		Map<String, SpringCliUserConfig.Host> hostsMap = new HashMap<>();
		hostsMap.put("github.com", new SpringCliUserConfig.Host("faketoken", "user"));
		hosts.setHosts(hostsMap);
		config.setHosts(hosts);
		assertThat(config.getHosts()).isNotNull();
		assertThat(config.getHosts().get("github.com")).isNotNull();
		assertThat(config.getHosts().get("github.com").getOauthToken()).isEqualTo("faketoken");
	}

	@Test
	public void testTemplateCatalogs() {
		SpringCliUserConfig config = new SpringCliUserConfig(pathProvider);

		assertThat(config.getTemplateCatalogs()).isNotNull();

		SpringCliUserConfig.TemplateCatalogs catalogs = new SpringCliUserConfig.TemplateCatalogs();
		List<SpringCliUserConfig.TemplateCatalog> catalogList = new ArrayList<>();
		SpringCliUserConfig.TemplateCatalog catalog1 = new SpringCliUserConfig.TemplateCatalog("fakename1", "fakedesc1", "fakeurl1");
		SpringCliUserConfig.TemplateCatalog catalog2 = new SpringCliUserConfig.TemplateCatalog("fakename2", "fakedesc2", "fakeurl2");
		catalogList.add(catalog1);
		catalogList.add(catalog2);
		catalogs.setTemplateCatalogs(catalogList);
		config.setTemplateCatalogs(catalogs);
		assertThat(config.getTemplateCatalogs().getTemplateCatalogs()).hasSize(2);
	}

	@Test
	public void testTemplateRepositories() {
		SpringCliUserConfig config = new SpringCliUserConfig(pathProvider);

		assertThat(config.getTemplateRepositories()).isNotNull();

		SpringCliUserConfig.TemplateRepositories repositories = new SpringCliUserConfig.TemplateRepositories();
		List<SpringCliUserConfig.TemplateRepository> repositoryList = new ArrayList<>();
		SpringCliUserConfig.TemplateRepository repository1 = new SpringCliUserConfig.TemplateRepository("fakename1", "fakedesc1", "fakeurl1", new ArrayList<>());
		SpringCliUserConfig.TemplateRepository repository2 = new SpringCliUserConfig.TemplateRepository("fakename2", "fakedesc2", "fakeurl2", new ArrayList<>());
		repositoryList.add(repository1);
		repositoryList.add(repository2);
		repositories.setTemplateRepositories(repositoryList);
		config.setTemplateRepositories(repositories);
		assertThat(config.getTemplateRepositories().getTemplateRepositories()).hasSize(2);
	}

	@Test
	public void testCommandDefaults() {
		SpringCliUserConfig springCliUserConfig = new SpringCliUserConfig(pathProvider);

		assertThat(springCliUserConfig.getCommandDefaults()).isNotNull();

		SpringCliUserConfig.CommandDefaults commandDefaults = new SpringCliUserConfig.CommandDefaults();
		List<SpringCliUserConfig.CommandDefault> commandDefaultList = new ArrayList<>();
		SpringCliUserConfig.CommandDefault commandDefault1 = new SpringCliUserConfig.CommandDefault("boot" ,"new");
		List<SpringCliUserConfig.Option> optionList = new ArrayList<>();
		optionList.add(new SpringCliUserConfig.Option("package-name", "com.xkcd"));
		commandDefault1.setOptions(optionList);
		commandDefaultList.add(commandDefault1);
		commandDefaults.setCommandDefaults(commandDefaultList);
		springCliUserConfig.setCommandDefaults(commandDefaults);
		assertThat(springCliUserConfig.getCommandDefaults().getCommandDefaults()).hasSize(1);

		//List<CommandDefault> commandDefaults1 = /commandDefaults.getCommandDefaults();
		Optional<String> defaultValue = commandDefaults.findDefaultOptionValue("boot", "new", "package-name");
		assertThat(defaultValue.get()).isEqualTo("com.xkcd");
	}

}

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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.up.support.configfile.UserConfig;
import org.springframework.util.ObjectUtils;

/**
 * Cli access point for user level stored settings.
 *
 * @author Janne Valkealahti
 */
public class UpCliUserConfig {

	/**
	 * Optional env variable for {@code SpringUp} configuration dir.
	 */
	public final static String SPRINGUP_CONFIG_DIR = "SPRINGUP_CONFIG_DIR";

	/**
	 * {@code hosts.yml} stores authentication spesific info for hosts.
	 */
	public final static String HOSTS = "hosts.yml";

	/**
	 * {@code catalogs.yml} stores template catalog spesific info.
	 */
	public final static String TEMPLATE_CATALOGS = "catalogs.yml";

	/**
	 * {@code repositories.yml} stores template repository spesific info.
	 */
	public final static String TEMPLATE_REPOSITORIES = "repositories.yml";

	/**
	 * Base directory name we store our config files.
	 */
	private final static String SPRINGUP_CONFIG_NAME = "springup";

	/**
	 * Keeps auth tokens per hostname.
	 */
	private final UserConfig<Hosts> hostsConfigFile;

	/**
	 * Keeps template catalogs as list.
	 */
	private final UserConfig<TemplateCatalogs> templateCatalogsConfigFile;

	/**
	 * Keeps template repositories as list.
	 */
	private final UserConfig<TemplateRepositories> templateRepositoriesConfigFile;

	public UpCliUserConfig() {
		this(null);
	}

	public UpCliUserConfig(Function<String, Path> pathProvider) {
		this.hostsConfigFile = new UserConfig<>(HOSTS, Hosts.class, SPRINGUP_CONFIG_DIR, SPRINGUP_CONFIG_NAME);
		this.templateCatalogsConfigFile = new UserConfig<>(TEMPLATE_CATALOGS, TemplateCatalogs.class,
				SPRINGUP_CONFIG_DIR, SPRINGUP_CONFIG_NAME);
		this.templateRepositoriesConfigFile = new UserConfig<>(TEMPLATE_REPOSITORIES, TemplateRepositories.class,
				SPRINGUP_CONFIG_DIR, SPRINGUP_CONFIG_NAME);
		if (pathProvider != null) {
			this.hostsConfigFile.setPathProvider(pathProvider);
			this.templateCatalogsConfigFile.setPathProvider(pathProvider);
			this.templateRepositoriesConfigFile.setPathProvider(pathProvider);
		}
	}

	/**
	 * Gets hosts.
	 *
	 * @return mappings for hosts
	 */
	public Map<String, Host> getHosts() {
		Hosts hosts = hostsConfigFile.getConfig();
		return hosts != null ? hosts.getHosts() : null;
	}

	/**
	 * Sets hosts.
	 *
	 * @param hosts
	 */
	public void setHosts(Hosts hosts) {
		hostsConfigFile.setConfig(hosts);
	}

	/**
	 * Update a single host.
	 *
	 * @param key the host key
	 * @param host the host
	 */
	public void updateHost(String key, Host host) {
		Map<String, Host> hostsMap = null;
		Hosts hosts = hostsConfigFile.getConfig();
		if (hosts != null) {
			hostsMap = hosts.getHosts();
		}
		else {
			hosts = new Hosts();
		}
		if (hostsMap == null) {
			hostsMap = new HashMap<>();
		}
		hostsMap.put(key, host);
		hosts.setHosts(hostsMap);
		setHosts(hosts);
	}

	/**
	 * Get template catalogs.
	 *
	 * @return template catalogs
	 */
	public TemplateCatalogs getTemplateCatalogsConfig() {
		TemplateCatalogs catalogs = templateCatalogsConfigFile.getConfig();
		return catalogs != null ? catalogs : new TemplateCatalogs();
	}

	/**
	 * Sets template catalogs
	 *
	 * @param templateCatalogs the template catalogs
	 */
	public void setTemplateCatalogsConfig(TemplateCatalogs templateCatalogs) {
		templateCatalogsConfigFile.setConfig(templateCatalogs);
	}

	/**
	 * Get template repositories.
	 *
	 * @return template repositories
	 */
	public TemplateRepositories getTemplateRepositoriesConfig() {
		TemplateRepositories repositories = templateRepositoriesConfigFile.getConfig();
		return repositories != null ? repositories : new TemplateRepositories();
	}

	/**
	 * Sets template repositories.
	 *
	 * @param templateRepositories
	 */
	public void setTemplateRepositoriesConfig(TemplateRepositories templateRepositories) {
		templateRepositoriesConfigFile.setConfig(templateRepositories);
	}

	public static class Hosts {

		private Map<String, Host> hosts = new HashMap<>();

		public Map<String, Host> getHosts() {
			return hosts;
		}

		public void setHosts(Map<String, Host> hosts) {
			this.hosts = hosts;
		}
	}

	public static class Host {

		private String oauthToken;
		private String user;

		public Host() {
		}

		public Host(String oauthToken, String user) {
			this.oauthToken = oauthToken;
			this.user = user;
		}

		public static Host of(String oauthToken, String user) {
			return new Host(oauthToken, user);
		}

		public String getOauthToken() {
			return oauthToken;
		}

		public void setOauthToken(String oauthToken) {
			this.oauthToken = oauthToken;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}
	}

	public abstract static class BaseTemplateCommon {

		private String name;
		private String description;
		private String url;

		BaseTemplateCommon() {
		}

		BaseTemplateCommon(String name, String description, String url) {
			this.name = name;
			this.description = description;
			this.url = url;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	public static class TemplateCatalogs {

		private List<TemplateCatalog> templateCatalogs = new ArrayList<>();

		public List<TemplateCatalog> getTemplateCatalogs() {
			return templateCatalogs;
		}

		public void setTemplateCatalogs(List<TemplateCatalog> templateCatalogs) {
			this.templateCatalogs = templateCatalogs;
		}
	}

	public static class TemplateRepositories {

		private List<TemplateRepository> templateRepositories = new ArrayList<>();

		public List<TemplateRepository> getTemplateRepositories() {
			return templateRepositories;
		}

		public void setTemplateRepositories(List<TemplateRepository> templateRepositories) {
			this.templateRepositories.clear();
			this.templateRepositories.addAll(templateRepositories);
		}

		public Optional<TemplateRepository> findByName(String name) {
			return this.templateRepositories.stream()
				.filter(t -> ObjectUtils.nullSafeEquals(t.getName(), name))
				.findFirst();
		}

		public void setTemplateRepository(TemplateRepository templateRepository) {
			templateRepositories.add(templateRepository);
		}
	}

	public static class TemplateCatalog extends BaseTemplateCommon {

		public TemplateCatalog() {
		}

		public TemplateCatalog(String name, String description, String url) {
			super(name, description, url);
		}

		public static TemplateCatalog of(String name, String description, String url) {
			return new TemplateCatalog(name, description, url);
		}
	}

	public static class TemplateRepository extends BaseTemplateCommon {

		private List<String> tags = new ArrayList<>();

		public TemplateRepository() {
		}

		public TemplateRepository(String name, String description, String url, List<String> tags) {
			super(name, description, url);
			this.tags = tags;
		}

		public static TemplateRepository of (String name, String description, String url, List<String> tags) {
			return new TemplateRepository(name, description, url, tags);
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}


	}
}

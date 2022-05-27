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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.cli.support.configfile.UserConfig;
import org.springframework.util.ObjectUtils;

/**
 * Cli access point for user level stored settings.
 *
 * @author Janne Valkealahti
 */
public class SpringCliUserConfig {

	/**
	 * Optional env variable for {@code Spring CLI} configuration dir.
	 */
	public final static String SPRING_CLI_CONFIG_DIR = "SPRING_CLI_CONFIG_DIR";

	/**
	 * {@code hosts.yml} stores authentication specific info for hosts.
	 */
	public final static String HOSTS_FILE_NAME = "hosts.yml";

	/**
	 * {@code catalogs.yml} stores template catalog specific info.
	 */
	public final static String TEMPLATE_CATALOGS_FILE_NAME = "catalogs.yml";

	/**
	 * {@code repositories.yml} stores template repository specific info.
	 */
	public final static String TEMPLATE_REPOSITORIES_FILE_NAME = "repositories.yml";

	/**
	 * {@code command-defaults.yml} store default option values for commands.
	 */
	public final static String COMMAND_DEFAULTS_FILE_NAME = "command-defaults.yml";
	/**
	 * Base directory name we store our config files.
	 */
	private final static String SPRING_CLI_CONFIG_DIR_NAME = "springcli";

	/**
	 * Keeps auth tokens per hostname.
	 */
	private final UserConfig<Hosts> hostsUserConfig;

	/**
	 * Keeps template catalogs as list.
	 */
	private final UserConfig<TemplateCatalogs> templateCatalogsUserConfig;

	/**
	 * Keeps template repositories as list.
	 */
	private final UserConfig<TemplateRepositories> templateRepositoriesUserConfig;

	private final UserConfig<CommandDefaults> commandDefaultsUserConfig;

	public SpringCliUserConfig() {
		this(null);
	}

	public SpringCliUserConfig(Function<String, Path> pathProvider) {
		this.hostsUserConfig = new UserConfig<>(HOSTS_FILE_NAME, Hosts.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.templateCatalogsUserConfig = new UserConfig<>(TEMPLATE_CATALOGS_FILE_NAME, TemplateCatalogs.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.templateRepositoriesUserConfig = new UserConfig<>(TEMPLATE_REPOSITORIES_FILE_NAME, TemplateRepositories.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.commandDefaultsUserConfig = new UserConfig<>(COMMAND_DEFAULTS_FILE_NAME, CommandDefaults.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		if (pathProvider != null) {
			this.hostsUserConfig.setPathProvider(pathProvider);
			this.templateCatalogsUserConfig.setPathProvider(pathProvider);
			this.templateRepositoriesUserConfig.setPathProvider(pathProvider);
			this.commandDefaultsUserConfig.setPathProvider(pathProvider);
		}
	}

	/**
	 * Gets hosts.
	 *
	 * @return mappings for hosts
	 */
	public Map<String, Host> getHosts() {
		Hosts hosts = hostsUserConfig.getConfig();
		return hosts != null ? hosts.getHosts() : null;
	}

	/**
	 * Sets hosts.
	 *
	 * @param hosts
	 */
	public void setHosts(Hosts hosts) {
		hostsUserConfig.setConfig(hosts);
	}

	/**
	 * Update a single host.
	 *
	 * @param key the host key
	 * @param host the host
	 */
	public void updateHost(String key, Host host) {
		Map<String, Host> hostsMap = null;
		Hosts hosts = hostsUserConfig.getConfig();
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
	public TemplateCatalogs getTemplateCatalogs() {
		TemplateCatalogs catalogs = templateCatalogsUserConfig.getConfig();
		return catalogs != null ? catalogs : new TemplateCatalogs();
	}

	/**
	 * Sets template catalogs
	 *
	 * @param templateCatalogs the template catalogs
	 */
	public void setTemplateCatalogs(TemplateCatalogs templateCatalogs) {
		templateCatalogsUserConfig.setConfig(templateCatalogs);
	}

	/**
	 * Get template repositories.
	 *
	 * @return template repositories
	 */
	// TODO rename to ProjectRepositories
	public TemplateRepositories getTemplateRepositories() {
		TemplateRepositories repositories = templateRepositoriesUserConfig.getConfig();
		return repositories != null ? repositories : new TemplateRepositories();
	}

	/**
	 * Sets template repositories.
	 *
	 * @param templateRepositories
	 */
	public void setTemplateRepositories(TemplateRepositories templateRepositories) {
		templateRepositoriesUserConfig.setConfig(templateRepositories);
	}

	/**
	 * Get command defaults
	 * @return command default
	 */
	public CommandDefaults getCommandDefaults() {
		CommandDefaults commandDefaults = this.commandDefaultsUserConfig.getConfig();
		return commandDefaults != null ? commandDefaults : new CommandDefaults();
	}

	/**
	 * Sets command defaults
	 *
	 * @param commandDefaults
	 */
	public void setCommandDefaults(CommandDefaults commandDefaults) {
		this.commandDefaultsUserConfig.setConfig(commandDefaults);
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

	public static class CommandDefaults {

		private List<CommandDefault> commandDefaults = new ArrayList<>();

		public List<CommandDefault> getCommandDefaults() {
			return commandDefaults;
		}

		public void setCommandDefaults(List<CommandDefault> commandDefaults) {
			this.commandDefaults = commandDefaults;
		}

		public Optional<String> findDefaultOptionValue(String commandName, String subCommandName, String optionName) {
			for (CommandDefault commandDefault : commandDefaults) {
				if (commandDefault.getCommandName().equals(commandName) &&
					commandDefault.getSubCommandName().equals(subCommandName)) {
					for (Option option : commandDefault.getOptions()) {
						if (option.getName().equals(optionName)) {
							return Optional.of(option.getValue());
						}
					}
				}
			}
			return Optional.empty();
		}
	}

	public static class CommandDefault {

		private String commandName;

		private String subCommandName;

		private List<Option> options = new ArrayList<>();

		public CommandDefault() {

		}
		public CommandDefault(String commandName, String subCommandName) {
			this.commandName = commandName;
			this.subCommandName = subCommandName;
		}

		public String getCommandName() {
			return commandName;
		}

		public void setCommandName(String commandName) {
			this.commandName = commandName;
		}

		public String getSubCommandName() {
			return subCommandName;
		}

		public void setSubCommandName(String subCommandName) {
			this.subCommandName = subCommandName;
		}

		public List<Option> getOptions() {
			return options;
		}

		public void setOptions(List<Option> options) {
			this.options = options;
		}
	}

	public static class Option {

		private String name;

		private String value;

		public Option() {

		}

		public Option(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return
					"'" + name + '\'' +
					" = '" + value + '\'';
		}
	}
}

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
	 * {@code catalogs.yml} stores project catalog specific info.
	 */
	public final static String PROJECT_CATALOGS_FILE_NAME = "project-catalogs.yml";

	/**
	 * {@code repositories.yml} stores project repository specific info.
	 */
	public final static String PROJECT_REPOSITORIES_FILE_NAME = "project-repositories.yml";

	/**
	 * {@code command-defaults.yml} store default option values for commands.
	 */
	public final static String COMMAND_DEFAULTS_FILE_NAME = "command-defaults.yml";

	/**
	 * {@code initializr.yml} stores initializr specific info.
	 */
	public final static String INITIALIZR_FILE_NAME = "initializr.yml";

	/**
	 * Base directory name we store our config files.
	 */
	private final static String SPRING_CLI_CONFIG_DIR_NAME = "springcli";

	/**
	 * Keeps auth tokens per hostname.
	 */
	private final UserConfig<Hosts> hostsUserConfig;

	/**
	 * Keeps project catalogs as list.
	 */
	private final UserConfig<ProjectCatalogs> projectCatalogsUserConfig;

	/**
	 * Keeps project repositories as list.
	 */
	private final UserConfig<ProjectRepositories> projectRepositoriesUserConfig;

	private final UserConfig<CommandDefaults> commandDefaultsUserConfig;

	private final UserConfig<Initializrs> initializrsUserConfig;


	public SpringCliUserConfig() {
		this(null);
	}

	public SpringCliUserConfig(Function<String, Path> pathProvider) {
		this.hostsUserConfig = new UserConfig<>(HOSTS_FILE_NAME, Hosts.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.projectCatalogsUserConfig = new UserConfig<>(PROJECT_CATALOGS_FILE_NAME, ProjectCatalogs.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.projectRepositoriesUserConfig = new UserConfig<>(PROJECT_REPOSITORIES_FILE_NAME, ProjectRepositories.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.commandDefaultsUserConfig = new UserConfig<>(COMMAND_DEFAULTS_FILE_NAME, CommandDefaults.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		this.initializrsUserConfig = new UserConfig<>(INITIALIZR_FILE_NAME, Initializrs.class,
				SPRING_CLI_CONFIG_DIR, SPRING_CLI_CONFIG_DIR_NAME);
		if (pathProvider != null) {
			this.hostsUserConfig.setPathProvider(pathProvider);
			this.projectCatalogsUserConfig.setPathProvider(pathProvider);
			this.projectRepositoriesUserConfig.setPathProvider(pathProvider);
			this.commandDefaultsUserConfig.setPathProvider(pathProvider);
			this.initializrsUserConfig.setPathProvider(pathProvider);
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
	 * Get project catalogs.
	 *
	 * @return project catalogs
	 */
	public ProjectCatalogs getProjectCatalogs() {
		ProjectCatalogs catalogs = projectCatalogsUserConfig.getConfig();
		return catalogs != null ? catalogs : new ProjectCatalogs();
	}

	/**
	 * Sets project catalogs
	 *
	 * @param projectCatalogs the project catalogs
	 */
	public void setProjectCatalogs(ProjectCatalogs projectCatalogs) {
		projectCatalogsUserConfig.setConfig(projectCatalogs);
	}

	/**
	 * Get project repositories.
	 *
	 * @return project repositories
	 */
	public ProjectRepositories getProjectRepositories() {
		ProjectRepositories repositories = projectRepositoriesUserConfig.getConfig();
		return repositories != null ? repositories : new ProjectRepositories();
	}

	/**
	 * Sets project repositories.
	 *
	 * @param projectRepositories
	 */
	public void setProjectRepositories(ProjectRepositories projectRepositories) {
		projectRepositoriesUserConfig.setConfig(projectRepositories);
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

	public Map<String, Initializr> getInitializrs() {
		Initializrs initializrs = initializrsUserConfig.getConfig();
		return initializrs != null ? initializrs.getInitializrs() : new HashMap<>();
	}

	public void setInitializrs(Initializrs initializrs) {
		initializrsUserConfig.setConfig(initializrs);
	}

	public void updateInitializr(String key, Initializr initializr) {
		Map<String, Initializr> initializrsMap = null;
		Initializrs initializrs = initializrsUserConfig.getConfig();
		if (initializrs != null) {
			initializrsMap = initializrs.getInitializrs();
		}
		else {
			initializrs = new Initializrs();
		}
		if (initializrsMap == null) {
			initializrsMap = new HashMap<>();
		}
		initializrsMap.put(key, initializr);
		initializrs.setInitializrs(initializrsMap);
		setInitializrs(initializrs);
	}

	public static class Initializrs {

		private Map<String, Initializr> initializrs = new HashMap<>();

		public Initializrs() {
		}

		public Initializrs(Map<String, Initializr> initializrs) {
			this.initializrs.putAll(initializrs);
		}

		public Map<String, Initializr> getInitializrs() {
			return initializrs;
		}

		public static Initializrs of(Map<String, Initializr> initializrs) {
			return new Initializrs(initializrs);
		}

		public void setInitializrs(Map<String, Initializr> initializrs) {
			this.initializrs = initializrs;
		}
	}

	public static class Initializr {

		private String url;

		Initializr() {
		}

		Initializr(String url) {
			this.url = url;
		}

		public static Initializr of(String url) {
			return new Initializr(url);
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	public abstract static class BaseProjectCommon {

		private String name;
		private String description;
		private String url;

		BaseProjectCommon() {
		}

		BaseProjectCommon(String name, String description, String url) {
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

	public static class ProjectCatalogs {

		private List<ProjectCatalog> projectCatalogs = new ArrayList<>();

		public List<ProjectCatalog> getProjectCatalogs() {
			return projectCatalogs;
		}

		public void setProjectCatalogs(List<ProjectCatalog> projectCatalogs) {
			this.projectCatalogs = projectCatalogs;
		}
	}

	public static class ProjectRepositories {

		private List<ProjectRepository> projectRepositories = new ArrayList<>();

		public List<ProjectRepository> getProjectRepositories() {
			return projectRepositories;
		}

		public void setProjectRepositories(List<ProjectRepository> projectRepositories) {
			this.projectRepositories.clear();
			this.projectRepositories.addAll(projectRepositories);
		}

		public Optional<ProjectRepository> findByName(String name) {
			return this.projectRepositories.stream()
				.filter(t -> ObjectUtils.nullSafeEquals(t.getName(), name))
				.findFirst();
		}

	}

	public static class ProjectCatalog extends BaseProjectCommon {

		private List<String> tags = new ArrayList<>();

		public ProjectCatalog() {
		}

		public ProjectCatalog(String name, String description, String url, List<String> tags) {
			super(name, description, url);
			this.tags = tags;
		}

		public static ProjectCatalog of(String name, String description, String url, List<String> tags) {
			return new ProjectCatalog(name, description, url, tags);
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}
	}

	public static class ProjectRepository extends BaseProjectCommon {

		private List<String> tags = new ArrayList<>();

		public ProjectRepository() {
		}

		public ProjectRepository(String name, String description, String url, List<String> tags) {
			super(name, description, url);
			this.tags = tags;
		}

		public static ProjectRepository of (String name, String description, String url, List<String> tags) {
			return new ProjectRepository(name, description, url, tags);
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

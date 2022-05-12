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
package org.springframework.up.support.configfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a single config file as a yml format.
 *
 * @author Janne Valkealahti
 */
public class UserConfig<T> {

	private final static String XDG_CONFIG_HOME = "XDG_CONFIG_HOME";
	private final static String APP_DATA = "APP_DATA";
	private Function<String, Path> pathProvider = (path) -> Paths.get(path);
	private final String name;
	private final ConfigFile file;
	private final Class<T> type;
	private final String configDirEnv;
	private final String configDirName;

	public UserConfig(String name, Class<T> type, String configDirEnv, String configDirName) {
		Assert.notNull(name, "name must be set");
		Assert.notNull(type, "type must be set");
		Assert.notNull(configDirEnv, "config dir env variable must be set");
		Assert.notNull(configDirName, "config dir name must be set");
		this.name = name;
		this.file = new YamlConfigFile();
		this.type = type;
		this.configDirEnv = configDirEnv;
		this.configDirName = configDirName;
	}

	public T getConfig() {
		Path path = getConfigDir().resolve(name);
		if (!Files.exists(path)) {
			return null;
		}
		return file.read(path, type);
	}

	public void setConfig(T config) {
		Path path = getConfigDir().resolve(name);
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException e) {
		}
		file.write(path, config);
	}

	/**
	 * Sets a path provider.
	 *
	 * @param pathProvider the path provider
	 */
	public void setPathProvider(Function<String, Path> pathProvider) {
		this.pathProvider = pathProvider;
	}

	private Path getConfigDir() {
		Path path;
		if (StringUtils.hasText(System.getenv(configDirEnv))) {
			path = pathProvider.apply(System.getenv(configDirEnv));
		}
		else if (StringUtils.hasText(System.getenv(XDG_CONFIG_HOME))) {
			path = pathProvider.apply(System.getenv(XDG_CONFIG_HOME)).resolve(configDirName);
		}
		else if (isWindows() && StringUtils.hasText(System.getenv(APP_DATA))) {
			path = pathProvider.apply(System.getenv(APP_DATA)).resolve(configDirName);
		}
		else {
			path = pathProvider.apply(System.getProperty("user.home")).resolve(".config").resolve(configDirName);
		}
		return path;
	}

	private boolean isWindows() {
		String os = System.getProperty("os.name");
		return os.startsWith("Windows");
	}
}

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

package org.springframework.cli.roles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor.ResolutionMethod;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.util.IoUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;

import static org.springframework.cli.util.JavaUtils.inferType;

public class RoleService {

	private Path workingDirectory = IoUtils.getWorkingDirectory();

	public RoleService() {
	}

	public RoleService(Path workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public Map<String, Object> loadAsMap(String name) {
		YamlMapFactoryBean factory = new YamlMapFactoryBean();
		factory.setResolutionMethod(ResolutionMethod.OVERRIDE_AND_IGNORE);
		factory.setResources(new FileSystemResource(getFile(name)));
		Map<String, Object> map = factory.getObject();
		return map;
	}

	public void updateRole(String roleName, String key, Object value) {
		createRolesDirectoryIfNecessary();

		// The default role is always updatable, create on demand.
		if (!StringUtils.hasText(roleName)) {
			File roleFile = getFile(roleName);
			if (!roleFile.exists()) {
				try {
					roleFile.createNewFile();
				}
				catch (IOException ex) {
					throw new SpringCliException(
							"Can not create role file for default role.  Message = " + ex.getMessage(), ex);
				}
			}

		}
		Map<String, Object> map = loadAsMap(roleName);
		Object valueToUse = inferType(value);
		map.put(key, valueToUse);

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		dumperOptions.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
		Yaml yaml = new Yaml(dumperOptions);
		File getRoleFile = getFile(roleName);
		try {
			yaml.dump(map, new PrintWriter(getRoleFile));
		}
		catch (FileNotFoundException e) {
			throw new SpringCliException(
					"The file for the role '" + roleName + "' was not found.  Error = " + e.getMessage());
		}

	}

	public List<String> getRoleNames(File directory) {
		List<String> roleNames = new ArrayList<>();
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			Pattern pattern = Pattern.compile("vars-(.*?)\\.(yml|yaml)");
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String filename = file.getName();
						Matcher matcher = pattern.matcher(filename);
						if (matcher.matches()) {
							String roleName = matcher.group(1);
							roleNames.add(roleName);
						}
					}
				}
			}
		}
		return roleNames;
	}

	public void createRolesDirectoryIfNecessary() {
		if (!IoUtils.inProjectRootDirectory(this.workingDirectory)) {
			throw new SpringCliException("You need to be in the root project directory to run 'role' commands.");
		}
		File directory = getRolesVarPath();
		if (!directory.exists()) {
			directory.mkdirs();
		}
	}

	/**
	 * Given the role name, return the file that contains the role variables. If the name
	 * is null or empty, the default file "vars.yml" is used. Otherwise, the file name is
	 * vars-{role}.yml
	 * @param name The name of the role. Empty string implies the default role
	 * @return the File with role variables.
	 */
	public File getFile(String name) {
		createRolesDirectoryIfNecessary();
		String fileName;
		if (StringUtils.hasText(name)) {
			fileName = "vars-" + name + ".yml";
		}
		else {
			fileName = "vars.yml";
		}
		String filePath = getRolesVarPath() + File.separator + fileName;
		File propertiesFile = new File(filePath);
		return propertiesFile;
	}

	public File getRolesVarPath() {
		return this.workingDirectory.resolve(".spring/roles/vars").toFile();
	}

}

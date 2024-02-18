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

package org.springframework.cli.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.roles.RoleService;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.StringUtils;

@Command(command = "role", group = "Role")
public class RoleCommands extends AbstractSpringCliCommands {

	private final TerminalMessage terminalMessage;

	private RoleService roleService;

	@Autowired
	public RoleCommands(TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
		this.roleService = new RoleService();
	}

	/**
	 * Primarily used for testing
	 * @param terminalMessage used to print to console
	 * @param workingDir where to run the command
	 */
	public RoleCommands(TerminalMessage terminalMessage, Path workingDir) {
		this.terminalMessage = terminalMessage;
		this.roleService = new RoleService(workingDir);
	}

	@Command(command = "add", description = "Add a role")
	public void roleAdd(@Option(description = "Role name", required = true) String name) {
		File roleFile = this.roleService.getFile(name);
		if (!roleFile.exists()) {
			try {
				this.roleService.createRolesDirectoryIfNecessary();
				roleFile.createNewFile();
				this.terminalMessage.print("Role '" + name + "' created.");
			}
			catch (IOException e) {
				String message = StringUtils.hasText(name) ? "role " + name : "the default role ";
				throw new SpringCliException("Error adding '" + message + ".  " + e.getMessage());
			}
		}
		else {
			this.terminalMessage.print("Role '" + name + "' already exists.");
		}
	}

	@Command(command = "remove", description = "Remove a named role")
	public void roleRemove(@Option(description = "Role name", required = true) String name) {
		File roleFile = this.roleService.getFile(name);
		if (roleFile.exists()) {
			if (roleFile.delete()) {
				String message = StringUtils.hasText(name) ? "Role '" + name + "'" : "The default role file was ";
				this.terminalMessage.print(message + " deleted.");
			}
		}
		else {
			this.terminalMessage.print("Role " + name + "' does not exist.");
		}
	}

	@Command(command = "set", description = "Set a key value pair for a role")
	public void roleSet(@Option(description = "A key", required = true) String key,
			@Option(description = "A value", required = true) Object value,
			@Option(description = "Role name") String name) {
		if (!StringUtils.hasText(name)) {
			name = "";
		}
		this.roleService.updateRole(name, key, value);
		String message = StringUtils.hasText(name) ? "to role '" + name + "'" : "to the default role";
		this.terminalMessage.print("Key-value pair added " + message);
	}

	@Command(command = "get", description = "Get the value of a key for a role")
	public void roleGet(@Option(description = "Property key", required = true) String key,
			@Option(description = "Role name") String name) {
		Map<String, Object> map = this.roleService.loadAsMap(name);
		if (!map.isEmpty()) {
			Object value = map.get(key);
			if (value != null) {
				this.terminalMessage.print(value.toString());
			}
			else {
				String message = StringUtils.hasText(name) ? " role " + name : "the default role";
				this.terminalMessage.print("Key '" + key + "' not found in " + message);
			}
		}
		else {
			String message = StringUtils.hasText(name) ? " role " + name : "the default role ";
			this.terminalMessage.print("Value for key " + key + " does not exist in " + message);
		}
	}

	@Command(command = "list", description = "List roles")
	public Table roleList() {
		File directory = this.roleService.getRolesVarPath();
		List<String> rolesNames = this.roleService.getRoleNames(directory);
		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name" });
		Stream<String[]> rows;
		if (rolesNames != null) {
			rows = rolesNames.stream().map(tr -> new String[] { tr });
		}
		else {
			rows = Stream.empty();
		}
		List<String[]> allRows = rows.collect(Collectors.toList());
		String[][] data = Stream.concat(header, allRows.stream()).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	public RoleService getRoleService() {
		return roleService;
	}

}

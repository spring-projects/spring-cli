/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.CommandDefault;
import org.springframework.cli.config.SpringCliUserConfig.CommandDefaults;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

//@Command(command = "config", group = "Config")
public class ConfigCommands extends AbstractSpringCliCommands {

	private SpringCliUserConfig springCliUserConfig;

	@Autowired
	public ConfigCommands(SpringCliUserConfig springCliUserConfig) {
		this.springCliUserConfig = springCliUserConfig;
	}

	// TODO - consider renaming to 'default set'
	@Command(command = "set", description = "For a given command name, set the default value for an option.")
	public void configSet(
			@Option(description = "Name of the command.") String commandName,
			@Option(description = "Name of the subcommand.") String subCommandName,
			@Option(description = "Name of the option.") String optionName,
			@Option(description = "Default value of the option.") String optionValue) {
		// get current defaults
		SpringCliUserConfig.CommandDefaults commandDefaults = new SpringCliUserConfig.CommandDefaults();
		List<CommandDefault> commandDefaultList = new ArrayList<>();
		SpringCliUserConfig.CommandDefault commandDefault = new SpringCliUserConfig.CommandDefault(commandName, subCommandName);
		List<SpringCliUserConfig.Option> optionList = new ArrayList<>();
		optionList.add(new SpringCliUserConfig.Option(optionName, optionValue));
		commandDefault.setOptions(optionList);
		commandDefaultList.add(commandDefault);
		commandDefaults.setCommandDefaults(commandDefaultList);
		this.springCliUserConfig.setCommandDefaults(commandDefaults);
	}

	@Command(command = "unset", description = "For a given command name, unset the default value for a given option.")
	public boolean configUnSet(
			@Option(description = "Name of the command.") String commandName,
			@Option(description = "Name of the subcommand.") String subCommandName,
			@Option(description = "Name of the option.") String optionName) {
		CommandDefaults commandDefaults = this.springCliUserConfig.getCommandDefaults();
		List<CommandDefault> commandDefaultList = commandDefaults.getCommandDefaults();
		Iterator<CommandDefault> it = commandDefaultList.iterator();
		boolean removed = false;
		while (it.hasNext()) {
			CommandDefault commandDefault = it.next();
			if (commandDefault.getCommandName().equals(commandName) &&
					commandDefault.getSubCommandName().equals(subCommandName)) {
				Iterator<org.springframework.cli.config.SpringCliUserConfig.Option> commandDefaultIterator = commandDefault.getOptions().iterator();
				while (commandDefaultIterator.hasNext()) {
					org.springframework.cli.config.SpringCliUserConfig.Option option = commandDefaultIterator.next();
					if (option.getName().equals(optionName)) {
						commandDefaultIterator.remove();
						removed = true;
					}
				}
				if (commandDefault.getOptions().size() == 0) {
					it.remove();
				}
			}
		}
		if (removed = true) {
			pruneEmptyOptions(commandDefaults);
			this.springCliUserConfig.setCommandDefaults(commandDefaults);
		}
		return false;
	}

	private void pruneEmptyOptions(CommandDefaults commandDefaults) {

	}

	@Command(command = "list", description = "List configuration values.")
	public Table configList() {

		Stream<String[]> header = Stream.<String[]>of(new String[] { "Command", "Sub Command", "Option Name/Values"});

		List<CommandDefault> commandDefaults = this.springCliUserConfig.getCommandDefaults().getCommandDefaults();
		Stream<String[]> rows = null;
		if (commandDefaults != null) {
			rows = commandDefaults.stream()
					.map(tr -> new String[] { tr.getCommandName(), tr.getSubCommandName(), tr.getOptions().toString() });
		}
		else {
			rows = Stream.empty();
		}
		String[][] data = Stream.concat(header, rows).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}
}

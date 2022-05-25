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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.support.AbstractSpringCliCommands;
import org.springframework.cli.support.SpringCliUserConfig;
import org.springframework.cli.support.SpringCliUserConfig.CommandDefault;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ConfigCommands extends AbstractSpringCliCommands {

	private static final Logger logger = LoggerFactory.getLogger(ConfigCommands.class);

	private SpringCliUserConfig springCliUserConfig;

	@Autowired
	public ConfigCommands(SpringCliUserConfig springCliUserConfig) {
		this.springCliUserConfig = springCliUserConfig;
	}

	@ShellMethod(key = "config set", value = "For a given command name, set a default value for an option")
	public void configSet(
			@ShellOption(help = "Name of command, specify as command-subcommand") String commandName,
			@ShellOption(help = "Name of option") String optionName,
			@ShellOption(help = "Default value of option") String optionValue) {
		// get current defaults
		System.out.println("commandName = " + commandName);
		System.out.println("optionName = " + optionName);
		System.out.println("default value = " + optionValue);
		SpringCliUserConfig.CommandDefaults commandDefaults = new SpringCliUserConfig.CommandDefaults();
		List<CommandDefault> commandDefaultList = new ArrayList<>();
		String commandNameToUse = commandName.replace("-", " ");
		SpringCliUserConfig.CommandDefault commandDefault = new SpringCliUserConfig.CommandDefault(commandNameToUse);
		List<SpringCliUserConfig.Option> optionList = new ArrayList<>();
		optionList.add(new SpringCliUserConfig.Option(optionName, optionValue));
		commandDefault.setOptions(optionList);
		commandDefaultList.add(commandDefault);
		commandDefaults.setCommandDefaults(commandDefaultList);
	}
}

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


package org.springframework.cli.runtime.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.roles.RoleService;
import org.springframework.cli.runtime.engine.actions.Action;
import org.springframework.cli.runtime.engine.actions.ActionFileReader;
import org.springframework.cli.runtime.engine.actions.ActionFileVisitor;
import org.springframework.cli.runtime.engine.actions.ActionsFile;
import org.springframework.cli.runtime.engine.actions.Exec;
import org.springframework.cli.runtime.engine.actions.Generate;
import org.springframework.cli.runtime.engine.actions.Inject;
import org.springframework.cli.runtime.engine.actions.InjectMavenBuildPlugin;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependency;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependencyManagement;
import org.springframework.cli.runtime.engine.actions.InjectMavenRepository;
import org.springframework.cli.runtime.engine.actions.Vars;
import org.springframework.cli.runtime.engine.actions.handlers.*;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.runtime.engine.spel.SpelFunctions;
import org.springframework.cli.runtime.engine.spel.SpELCondition;
import org.springframework.cli.runtime.engine.templating.HandlebarsTemplateEngine;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.NamingUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandParser.CommandParserResult;
import org.springframework.util.StringUtils;

/**
 * Object that is registered and executed for all dynamic commands discovered
 * at runtime.
 */
public class DynamicCommand {

	private static final Logger logger = LoggerFactory.getLogger(DynamicCommand.class);

	private final String commandName;

	private final String subCommandName;

	private final Iterable<ModelPopulator> modelPopulators;

	private final TerminalMessage terminalMessage;

	private final TemplateEngine templateEngine;

	private final Optional<Terminal> terminalOptional;

	public DynamicCommand(String commandName, String subCommandName,
			Iterable<ModelPopulator> modelPopulators,
			TerminalMessage terminalMessage,
			Optional<Terminal> terminalOptional) {
		this.commandName = commandName;
		this.subCommandName = subCommandName;
		this.modelPopulators = modelPopulators;
		this.terminalMessage = terminalMessage;
		this.templateEngine = new HandlebarsTemplateEngine();
		this.terminalOptional = terminalOptional;
	}

	/**
	 * The main method called by spring-shell.  It is *not* an unused method, see
	 * DynamicMethodCommandResolver for usage.
	 * @param commandContext the command context for the dynamic command.
	 */
	public void execute(CommandContext commandContext) {
		Map<String, Object> model = new HashMap<>();
		addMatchedOptions(model, commandContext);
		addRoleVariables(model, commandContext);
		runCommand(IoUtils.getWorkingDirectory(), ".spring", "commands", model);
	}
	private void addMatchedOptions(Map<String, Object> model, CommandContext commandContext) {
		List<CommandParserResult> commandParserResults = commandContext.getParserResults().results();
		for (CommandParserResult commandParserResult : commandParserResults) {
			String kebabOption = NamingUtils.toKebab(commandParserResult.option().getLongNames()[0]);
			// TODO remove toString to use type info
			// This puts in default values as well.
			model.put(kebabOption, commandParserResult.value().toString());
		}
	}

	/**
	 * Adds to the model, values from the vars-{role}.yml file.
	 * If any command line argument values were not explicitly passed in but are present
	 * in the vars-{role}.yml file, the key-value pair will be present in the model available to actions.
	 * If the command line argument was explicitly passed in and also present in the
	 * vars-{role}.yml file, the key-value pair that was explicitly passed in is used and not
	 * the key-value pair from the vars-{role}.yml file.
	 * @param model  The model available to actions
	 * @param commandContext The context of what CLI arguments were passed in.
	 */
	private void addRoleVariables(Map<String, Object> model, CommandContext commandContext) {
		RoleService roleService = new RoleService();
		String role = "";
		if (model.containsKey("role")) {
			role = (String) model.get("role");
		}
		File roleFile = roleService.getFile(role);
		if (roleFile.exists()) {
			Map<String, Object> roleVariableMap = roleService.loadAsMap(role);
			for (Entry<String, Object> roleMapEntry : roleVariableMap.entrySet()) {
				String roleKey = roleMapEntry.getKey();
				Object roleValue = roleMapEntry.getValue();
				boolean usedDefaultValue = usedDefaultValue(roleKey, commandContext);
				// Do not add if the option name is already there due to passing from the command line options
				if (usedDefaultValue) {
					// Override the default roleValue with the roleValue from the Role Variable.
					model.put(roleKey, roleValue);
					String message = StringUtils.hasText(role) ? " role " + role : "the default role ";
					this.terminalMessage.print("Using Role variable instead of default command line option for roleKey = " + roleKey + " , roleValue = " + roleValue + " from " + message);
				}
			}
			// Insert map variables that are independent of the command line option names
			for (Entry<String, Object> roleMapEntry : roleVariableMap.entrySet()) {
				String roleKey = roleMapEntry.getKey();
				Object roleValue = roleMapEntry.getValue();
				model.putIfAbsent(roleKey, roleValue);
			}
		} else {
			if (StringUtils.hasText(role)) {
				this.terminalMessage.print("File for role '" + role + "' does not exist.  Create role using 'role add'");
			}
		}

	}

	private boolean usedDefaultValue(String variableName, CommandContext commandContext) {
		boolean usedDefaultValue = false;
		// Look for matching option of the provided variable name and determine if a default value was used.
		List<CommandParserResult> commandParserResults = commandContext.getParserResults().results();
		for (CommandParserResult commandParserResult : commandParserResults) {
			String optionName = NamingUtils.toKebab(commandParserResult.option().getLongNames()[0]);
			if (variableName.equals(optionName)) {
				Object defaultOptionValue = commandParserResult.option().getDefaultValue();
				Object optionValue = commandParserResult.value();
				if (defaultOptionValue != null && optionValue != null && defaultOptionValue.equals(optionValue) ) {
						usedDefaultValue = true;
				}
			}
		}
		return usedDefaultValue;
	}

	public void runCommand(Path workingDirectory, String springDir, String commandsDir,
			Map<String, Object> model)  {
		Path dynamicSubCommandPath;
		if (StringUtils.hasText(springDir) && StringUtils.hasText(commandsDir)) {
			dynamicSubCommandPath = Paths.get(workingDirectory.toString(), springDir, commandsDir)
					.resolve(this.commandName).resolve(this.subCommandName).toAbsolutePath();
		} else {
			// Used in testing w/o .spring/commands subdirectories
			dynamicSubCommandPath = Paths.get(workingDirectory.toString())
					.resolve(this.commandName).resolve(this.subCommandName).toAbsolutePath();
		}

		// Enrich the model with detected features of the project, e.g. maven artifact name
		if (this.modelPopulators != null) {
			for (ModelPopulator modelPopulator : modelPopulators) {
				modelPopulator.contributeToModel(workingDirectory, model);
			}
		}

		final Map<Path, ActionsFile> commandActionFiles = findCommandActionFiles(dynamicSubCommandPath);
		if (commandActionFiles.size() == 0) {
			throw new SpringCliException("No command action files found to process in directory " + dynamicSubCommandPath.toAbsolutePath());
		}

		try {
			processCommandActionFiles(commandActionFiles, workingDirectory, dynamicSubCommandPath, model);
		} catch (SpringCliException e) {
			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.RED));
			sb.append(e.getMessage());
			terminalMessage.print(sb.toAttributedString());
		}

	}

	private void processCommandActionFiles(Map<Path, ActionsFile> commandActionFiles, Path cwd, Path dynamicSubCommandPath, Map<String, Object> model) {

		for (Entry<Path, ActionsFile> kv : commandActionFiles.entrySet()) {
			Path path = kv.getKey();
			ActionsFile actionsFile = kv.getValue();

			List<Action> actions = actionsFile.getActions();
			if (actions.isEmpty()) {
				terminalMessage.print("No actions to execute in " + path.toAbsolutePath());
				continue;
			}

			for (Action action : actions) {

				RoleService roleService = new RoleService(cwd);
				// TODO load from default role for now, later a list of roles
				Map<String, Object> varMap = roleService.loadAsMap("");
				model.putAll(varMap);

				String ifExpression = action.getIfExpression();
				if (StringUtils.hasText(ifExpression)) {
					// Prepare to execute expression evaluation
					String ifExpressionToUse = this.templateEngine.process(ifExpression, model);
					ExecActionHandler execActionHandler = new ExecActionHandler(templateEngine, model, dynamicSubCommandPath, this.terminalMessage);
					model.put("functions", new SpelFunctions(execActionHandler, cwd));

					SpELCondition condition = new SpELCondition(ifExpressionToUse);
					boolean evaluationResult = condition.evaluate(model);
					logger.debug("If Expression = " + ifExpression);
					logger.debug("Evaluation Result " + evaluationResult);
					if (!evaluationResult) {
						logger.debug("Not execution action, evaluation result = false");
						continue;
					}
				}

				Generate generate = action.getGenerate();
				if (generate != null) {
					GenerateActionHandler generateActionHandler = new GenerateActionHandler(templateEngine, model, cwd, dynamicSubCommandPath, terminalMessage);
					generateActionHandler.execute(generate);
				}

				Inject inject = action.getInject();
				if (inject != null) {
					InjectActionHandler injectActionHandler = new InjectActionHandler(templateEngine, model, cwd, terminalMessage);
					injectActionHandler.execute(inject);
				}


				InjectMavenDependency injectMavenDependency = action.getInjectMavenDependency();
				if (injectMavenDependency != null) {
					InjectMavenActionHandler injectMavenActionHandler = new InjectMavenActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenActionHandler.injectDependency(injectMavenDependency);
					injectMavenActionHandler.exec();
				}

				InjectMavenBuildPlugin injectMavenBuildPlugin = action.getInjectMavenBuildPlugin();
				if (injectMavenBuildPlugin != null) {
					InjectMavenActionHandler injectMavenActionHandler = new InjectMavenActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenActionHandler.injectBuildPlugin(injectMavenBuildPlugin);
					injectMavenActionHandler.exec();
				}

				InjectMavenDependencyManagement injectMavenDependencyManagement = action.getInjectMavenDependencyManagement();
				if (injectMavenDependencyManagement != null) {
					InjectMavenActionHandler injectMavenActionHandler = new InjectMavenActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenActionHandler.injectDependencyManagement(injectMavenDependencyManagement);
					injectMavenActionHandler.exec();
				}

				InjectMavenRepository injectMavenRepository = action.getInjectMavenRepository();
				if (injectMavenRepository != null) {
					InjectMavenActionHandler injectMavenActionHandler = new InjectMavenActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenActionHandler.injectRepository(injectMavenRepository);
					injectMavenActionHandler.exec();
				}

				Exec exec = action.getExec();
				if (exec != null) {
					ExecActionHandler execActionHandler = new ExecActionHandler(templateEngine, model, dynamicSubCommandPath, terminalMessage);
					Map<String, Object> outputs = new HashMap<>();
					execActionHandler.executeShellCommand(exec, outputs);
				}

				Vars vars = action.getVars();
				if (vars != null) {
					VarsActionHandler varsActionHandler = new VarsActionHandler(templateEngine, model, cwd, dynamicSubCommandPath, terminalMessage, terminalOptional.get());
					varsActionHandler.execute(vars);
				}
			}
		}


	}
	private Map<Path, ActionsFile> findCommandActionFiles(Path dynamicSubCommandPath) {
		// Do a first pass to find only text files
		final ActionFileVisitor visitor = new ActionFileVisitor();
		try {
			Files.walkFileTree(dynamicSubCommandPath, visitor);
		}
		catch (IOException e) {
			throw new SpringCliException("Error trying to detect action files. " + e.getMessage(), e);
		}

		// Then actually parse, retaining only those paths that yielded a result
		ActionFileReader actionFileReader = new ActionFileReader();
		return visitor.getMatches().stream() //
				.map(p -> new SimpleImmutableEntry<>(p, actionFileReader.read(p))) //
				.filter(kv -> kv.getValue().isPresent()) //
				.collect(toSortedMap(Entry::getKey, (e) -> e.getValue().get()));
	}

	private static <T, K, U> Collector<T, ?, Map<K, U>> toSortedMap(Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return Collectors.toMap(keyMapper, valueMapper, (v1, v2) -> {
			throw new IllegalStateException(String.format("Duplicate key for values %s and %s", v1, v2));
		}, TreeMap::new);
	}

}

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.actions.Action;
import org.springframework.cli.runtime.engine.actions.ActionFileReader;
import org.springframework.cli.runtime.engine.actions.ActionFileVisitor;
import org.springframework.cli.runtime.engine.actions.ActionsFile;
import org.springframework.cli.runtime.engine.actions.Conditional;
import org.springframework.cli.runtime.engine.actions.Exec;
import org.springframework.cli.runtime.engine.actions.Generate;
import org.springframework.cli.runtime.engine.actions.Inject;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependency;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependencyManagement;
import org.springframework.cli.runtime.engine.actions.InjectMavenRepository;
import org.springframework.cli.runtime.engine.actions.handlers.ExecActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.GenerateActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.InjectActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.InjectMavenDependencyActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.InjectMavenDependencyManagementActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.InjectMavenRepositoryActionHandler;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.runtime.engine.templating.HandlebarsTemplateEngine;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.NamingUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandParser.CommandParserResult;
import org.springframework.util.StringUtils;

import static org.springframework.cli.runtime.engine.model.MavenModelPopulator.MAVEN_MODEL;

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


	public DynamicCommand(String commandName, String subCommandName, Iterable<ModelPopulator> modelPopulators, TerminalMessage terminalMessage) {
		this.commandName = commandName;
		this.subCommandName = subCommandName;
		this.modelPopulators = modelPopulators;
		this.terminalMessage = terminalMessage;
		this.templateEngine = new HandlebarsTemplateEngine();
	}

	/**
	 * The main method called by spring-shell.  It is *not* and unused method, see
	 * DynamicMethodCommandResolver for usage.
	 * @param commandContext the command context for the dynamic command.
	 */
	public void execute(CommandContext commandContext) {
		Map<String, Object> model = new HashMap<>();
		addMatchedOptions(model, commandContext);
		runCommand(IoUtils.getWorkingDirectory(), ".spring", "commands", model);
	}


	private void addMatchedOptions(Map<String, Object> model, CommandContext commandContext) {
		List<CommandParserResult> commandParserResults = commandContext.getParserResults().results();
		for (CommandParserResult commandParserResult : commandParserResults) {
			// TODO will value() be populated with defaultValue() if not passed in?
			String kebabOption = NamingUtils.toKebab(commandParserResult.option().getLongNames()[0]);
			// TODO will value() be populated with defaultValue() if not passed in?
			model.put(kebabOption, commandParserResult.value().toString());
		}
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

			if (actionsFile.getConditional() != null) {
				checkConditional(actionsFile.getConditional(), model);
			}

			List<Action> actions = actionsFile.getActions();
			if (actions.isEmpty()) {
				terminalMessage.print("No actions to execute in " + path.toAbsolutePath());
				continue;
			}

			for (Action action : actions) {
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
					InjectMavenDependencyActionHandler injectMavenDependencyActionHandler = new InjectMavenDependencyActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenDependencyActionHandler.execute(injectMavenDependency);
				}

				InjectMavenDependencyManagement injectMavenDependencyManagement = action.getInjectMavenDependencyManagement();
				if (injectMavenDependencyManagement != null) {
					InjectMavenDependencyManagementActionHandler injectMavenDependencyDependnecyActionHandler
							= new InjectMavenDependencyManagementActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenDependencyDependnecyActionHandler.execute(injectMavenDependencyManagement);
				}

				InjectMavenRepository injectMavenRepository = action.getInjectMavenRepository();
				if (injectMavenRepository != null) {
					InjectMavenRepositoryActionHandler injectMavenRepositoryActionHandler =
							new InjectMavenRepositoryActionHandler(templateEngine, model, cwd, terminalMessage);
					injectMavenRepositoryActionHandler.execute(injectMavenRepository);
				}

				Exec exec = action.getExec();
				if (exec != null) {
					ExecActionHandler execActionHandler = new ExecActionHandler(templateEngine, model, dynamicSubCommandPath, terminalMessage);
					execActionHandler.executeShellCommand(exec);
				}

			}
		}


	}

	private void checkConditional(Conditional conditional, Map<String, Object> model) {
		Model mavenModel = (Model) model.get(MAVEN_MODEL);
		String artifactId = conditional.getArtifactId();
		if (StringUtils.hasText(artifactId) && mavenModel != null) {
			boolean hasArtifactId = mavenModel.getDependencies().stream()
					.anyMatch((dependency) -> dependency.getArtifactId().equalsIgnoreCase(artifactId.trim()));
			if (!hasArtifactId) {
				throw new SpringCliException("Conditional on artifact-id not satisfied.  Expected artifact-id " + artifactId.trim() + " but was not found.");
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
		return visitor.getMatches().stream() //
				.map((p) -> new SimpleImmutableEntry<>(p, ActionFileReader.read(p))) //
				.filter((kv) -> kv.getValue().isPresent()) //
				.collect(toSortedMap(Entry::getKey, (e) -> e.getValue().get()));
	}

	private static <T, K, U> Collector<T, ?, Map<K, U>> toSortedMap(Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return Collectors.toMap(keyMapper, valueMapper, (v1, v2) -> {
			throw new IllegalStateException(String.format("Duplicate key for values %s and %s", v1, v2));
		}, TreeMap::new);
	}

}

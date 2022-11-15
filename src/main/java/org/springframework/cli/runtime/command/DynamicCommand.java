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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.frontmatter.Action;
import org.springframework.cli.runtime.engine.frontmatter.CommandActionFileContents;
import org.springframework.cli.runtime.engine.frontmatter.Exec;
import org.springframework.cli.runtime.engine.frontmatter.FrontMatter;
import org.springframework.cli.runtime.engine.frontmatter.FrontMatterFileVisitor;
import org.springframework.cli.runtime.engine.frontmatter.FrontMatterReader;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.runtime.engine.templating.HandlebarsTemplateEngine;
import org.springframework.cli.runtime.engine.templating.MustacheTemplateEngine;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.IoUtils;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandParser.CommandParserResult;
import org.springframework.util.StringUtils;

/**
 * Object that is registered and executed for all dynamic commands discovered
 * at runtime.
 *
 * It uses the GeneratorResolver to read files contained in the command/subcommand directory
 * as specified by the arguments commandName and subCommandName
 *
 * Delegates to
 * and interprets files

 */
public class DynamicCommand {

	private static final Logger logger = LoggerFactory.getLogger(DynamicCommand.class);

	private String commandName;

	private String subCommandName;

	private Iterable<ModelPopulator> modelPopulators;

	public DynamicCommand(String commandName, String subCommandName, Iterable<ModelPopulator> modelPopulators) {
		this.commandName = commandName;
		this.subCommandName = subCommandName;
		this.modelPopulators = modelPopulators;

	}

	public void execute(CommandContext commandContext) throws IOException {

		//System.out.println("Hello world from dynamic command " + commandName + " " + subCommandName);

		Map<String, Object> model = new HashMap<>();

		addMatchedOptions(model, commandContext);

		runCommand(IoUtils.getWorkingDirectory(), ".spring", "commands", commandContext, model);
	}


	private void addMatchedOptions(Map<String, Object> model, CommandContext commandContext) {
		List<CommandParserResult> commandParserResults = commandContext.getParserResults().results();
		for (CommandParserResult commandParserResult : commandParserResults) {
			// TODO will value() be populated with defaultValue() if not passed in?
			String kebabOption = toKebab(commandParserResult.option().getLongNames()[0]);
			// TODO will value() be populated with defaultValue() if not passed in?
			model.put(kebabOption, commandParserResult.value().toString());
		}
	}


	public void runCommand(Path workingDirectory, String springDir, String commandsDir,
			CommandContext commandContext, Map<String, Object> model) throws IOException {
		Path dynamicSubCommandPath;
		if (StringUtils.hasText(springDir) && StringUtils.hasText(commandsDir)) {
			dynamicSubCommandPath = Paths.get(workingDirectory.toString(), springDir, commandsDir)
					.resolve(this.commandName).resolve(this.subCommandName).toAbsolutePath();
		} else {
			dynamicSubCommandPath = Paths.get(workingDirectory.toString())
					.resolve(this.commandName).resolve(this.subCommandName).toAbsolutePath();
		}


		// Enrich the model with detected features of the project, e.g. maven artifact name
		if (this.modelPopulators != null) {
			for (ModelPopulator modelPopulator : modelPopulators) {
				modelPopulator.contributeToModel(workingDirectory, model);
			}
		}
		//System.out.println(model);

		// TODO normalize model keys to camelCase?

		final Map<Path, CommandActionFileContents> commandActionFiles = findCommandActionFiles(dynamicSubCommandPath);
		if (commandActionFiles.size() == 0) {
			throw new SpringCliException("No command action files found to process in directory " + dynamicSubCommandPath.toAbsolutePath());
		}

		processCommandActionFiles(commandActionFiles, workingDirectory, model);


	}

	private void processCommandActionFiles(Map<Path, CommandActionFileContents> commandActionFiles, Path cwd, Map<String, Object> model) throws IOException {

		for (Entry<Path, CommandActionFileContents> kv : commandActionFiles.entrySet()) {
			Path path = kv.getKey();
			CommandActionFileContents commandActionFileContents = kv.getValue();
			Action action = commandActionFileContents.getFrontMatter().getAction();
			if (action == null) {
				System.out.println("No actions to execute in " + path.toAbsolutePath());
				continue;
			}
			TemplateEngine templateEngine = getTemplateEngine(commandActionFileContents.getFrontMatter());

			// Add conditional execution

			String generate = action.getGenerate();
			if (StringUtils.hasText(generate)) {
				String toFileName = templateEngine.process(generate, model);
				if (StringUtils.hasText(toFileName)) {
					generateFile(commandActionFileContents, templateEngine, toFileName, action.isOverwrite(), model, cwd);
				}
			}

			Exec exec = action.getExec();
			if (exec != null) {
				executeShellCommand(exec, templateEngine, model, commandActionFileContents);
			}

		}


	}

	private void executeShellCommand(Exec exec, TemplateEngine templateEngine, Map<String, Object> model, CommandActionFileContents commandActionFileContents) {
		List<String> args = exec.getArgs();
		if (args.size() == 0) {
			// TODO indicate which action file has an issue.
			throw new SpringCliException("No arguments provided to exec command.  First argument is usually the name of the executable.  Action File contents = " + commandActionFileContents.getText());
		}
		List<String> processedArgs = new ArrayList<>();

		for (String arg : args) {
			try {
				String objectResult = templateEngine.process(arg, model);
				processedArgs.add(objectResult);
			}
			catch (Exception ex) {
				throw new SpringCliException("Error evaluating exec argument expression. Expression: " + arg, ex);
			}
		}
		String[] actionCommandArgs = processedArgs.toArray(new String[0]);
		ProcessBuilder processBuilder = new ProcessBuilder(actionCommandArgs);
		try {
			String dir = templateEngine.process(exec.getDir(), model);
			processBuilder.directory(new File(dir).getCanonicalFile());
		}
		catch (Exception e) {
			throw new SpringCliException("Error evaluating exec working directory. Expression: " + exec.getDir(), e);
		}

		// If exec.getTo is set, it is the relative path to which to redirect stdout of the running process.
		if (exec.getTo() != null) {
			try {
				String execto = templateEngine.process(exec.getTo(), model);
				processBuilder.redirectOutput(new File(execto));
			}
			catch (Exception e) {
				throw new SpringCliException("Error evaluating exec destination file. Expression: " + exec.getTo(),
						e);
			}
		}

		// If exec.getErrto() is set, the relative path to which to redirect stderr of the running process.
		if (exec.getErrto() != null) {
			try {
				String execerrto = templateEngine.process(exec.getErrto(), model);
				processBuilder.redirectError(new File(execerrto));
			}
			catch (Exception e) {
				throw new SpringCliException("Error evaluating exec error file. Expression: " + exec.getErrto(), e);
			}
		}


		Path tmpDir = null;
		//	 If exec.isIn is true, use the rendered body of the template as stdin of the running process.
		if (exec.isIn()) {

			try {
				tmpDir = Files.createTempDirectory("exec");
			}
			catch (IOException e) {
				throw new SpringCliException("Could not create temp directory.", e);
			}
			try {
				generateFile(commandActionFileContents, templateEngine, "stdin", true, model, tmpDir);
			}
			catch (IOException e) {
				throw new SpringCliException("Could not generate stdin file.", e);
			}
			processBuilder.redirectInput(tmpDir.resolve("stdin").toFile());
		}

		try {
			System.out.println("Executing: " + StringUtils.collectionToDelimitedString(processedArgs, " ") );
			Process process = processBuilder.start();
			// capture the output.
			String stderr = "";
			String stdout = "";
			if (exec.getTo() == null && exec.getErrto() == null) {
				stdout = readStringFromInputStream(process.getInputStream());
				stderr = readStringFromInputStream(process.getErrorStream());
			}

			boolean exited = process.waitFor(300, TimeUnit.SECONDS);
			if (exited) {
				if (process.exitValue() == 0) {
					//TODO better writing out to terminal
					System.out.println("Command '" + StringUtils.collectionToDelimitedString(processedArgs, " ") + "' executed successfully");
					if (exec.getDefine() != null) {
						if (exec.getDefine().getName() != null && exec.getDefine().getJsonPath() != null) {
							ObjectMapper mapper = new ObjectMapper();
							mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
							mapper.registerModule(new JavaTimeModule());
							Object data = JsonPath.using(
									Configuration.builder()
											.jsonProvider(new JacksonJsonProvider(mapper))
											.mappingProvider(new JacksonMappingProvider(mapper))
											.build()
							).parse(stdout).read(exec.getDefine().getJsonPath());
							if (data != null) {
								model.putIfAbsent(exec.getDefine().getName(), data);
							}
						} else {
							System.out.println("exec: define: has a null value.  Define = " + exec.getDefine());
						}
					}
				}
				else {
					System.out.println("Command '" + StringUtils.collectionToDelimitedString(processedArgs, " ") + "' exited with value " + process.exitValue());
					System.out.println("stderr = " + stderr);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SpringCliException("Execution of command '"
					+ StringUtils.collectionToDelimitedString(processedArgs, " ") + "' failed", e);

		}
		catch (IOException e) {
			throw new SpringCliException("Execution of command '"
					+ StringUtils.collectionToDelimitedString(processedArgs, " ") + "' failed", e);
		}
		finally {
			if (tmpDir != null) {
				try {
					Files.deleteIfExists(tmpDir.resolve("stdin"));
					Files.deleteIfExists(tmpDir);
				} catch (IOException e) {
					System.err.println("Could not delete temp directory " + tmpDir);
				}
			}
		}

	}

	private String readStringFromInputStream(InputStream input) {
		final String newline = System.getProperty("line.separator");
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return buffer.lines().collect(Collectors.joining(newline));
		}
		catch (IOException e) {
			logger.error("Could not read command output: " + e.getMessage());
		}
		return null;
	}

	private void generateFile(CommandActionFileContents commandActionFileContents, TemplateEngine templateEngine, String toFileName, boolean overwrite, Map<String, Object> model, Path cwd) throws IOException {
		Path pathToFile = cwd.resolve(toFileName).toAbsolutePath();
		if ((pathToFile.toFile().exists() && overwrite) || (!pathToFile.toFile().exists())) {
			writeFile(commandActionFileContents, templateEngine, model, pathToFile);
		}
		else {
			System.out.println("Skipping generation of " + pathToFile);
		}
	}

	private void writeFile(CommandActionFileContents commandActionFileContents, TemplateEngine templateEngine,
			Map<String, Object> model, Path pathToFile) throws IOException {
		Files.createDirectories(pathToFile.getParent());
		String result = templateEngine.process(commandActionFileContents.getText(), model);
		Files.write(pathToFile, result.getBytes());
		// TODO: keep log of action taken so can report later.
		System.out.println("Generated "+ pathToFile);
	}

	private TemplateEngine getTemplateEngine(FrontMatter frontMatter) {
		String engine = frontMatter.getEngine();
		TemplateEngine templateEngine;
		if (engine.equalsIgnoreCase("mustache")) {
			templateEngine = new MustacheTemplateEngine();
		}
		else {
			templateEngine = new HandlebarsTemplateEngine();
		}
		return templateEngine;
	}

	private Optional<CommandFileContents> getCommandFileContents(Path dynamicSubCommandPath) {
		Path commandFilePath = dynamicSubCommandPath.resolve("command.yaml");
		if (!commandFilePath.toFile().exists()) {
			return Optional.empty();
		}
		try {
			return Optional.of(CommandFileReader.read(commandFilePath));
		}
		catch (IOException e) {
			logger.warn("Could not read command.yaml in path {}", commandFilePath, e);
			return Optional.empty();
		}
	}

	private Map<Path, CommandActionFileContents> findCommandActionFiles(Path dynamicSubCommandPath) {
		// Do a first pass to find all files that look parseable...
		final FrontMatterFileVisitor visitor = new FrontMatterFileVisitor();
		try {
			Files.walkFileTree(dynamicSubCommandPath, visitor);
		}
		catch (IOException e) {
			throw new SpringCliException("Error trying to detect action files", e);
		}

		// Then actually parse, retaining only those paths that yielded a result
		return visitor.getMatches().stream() //
				.map((p) -> new SimpleImmutableEntry<>(p, FrontMatterReader.read(p))) //
				.filter((kv) -> kv.getValue().isPresent()) //
				.collect(toSortedMap(Entry::getKey, (e) -> e.getValue().get()));
	}

	private static <T, K, U> Collector<T, ?, Map<K, U>> toSortedMap(Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return Collectors.toMap(keyMapper, valueMapper, (v1, v2) -> {
			throw new IllegalStateException(String.format("Duplicate key for values %s and %s", v1, v2));
		}, TreeMap::new);
	}


	public static String toKebab(CharSequence original) {
		StringBuilder result = new StringBuilder(original.length());
		boolean wasLowercase = false;
		for (int i = 0; i < original.length(); i++) {
			char ch = original.charAt(i);
			if (Character.isUpperCase(ch) && wasLowercase) {
				result.append('-');
			}
			wasLowercase = Character.isLowerCase(ch);
			result.append(Character.toLowerCase(ch));
		}
		return result.toString();
	}
}

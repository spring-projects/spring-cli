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


package org.springframework.cli.runtime.engine.actions.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
import org.springframework.cli.runtime.engine.actions.Exec;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

/**
 * Responsible for processing logic behind the 'exec' action
 */
public class ExecActionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ExecActionHandler.class);

	public final static String OUTPUT_STDOUT = "stdout";

	public final static String OUTPUT_STDERR = "stderr";

	public final static String OUTPUT_EXIT_VALUE = "exit-value";

	public final static String OUTPUT_STDOUT_JSONPATH = "stdout-json-path";

	private final TemplateEngine templateEngine;

	private final Map<String, Object> model;

	private final Path dynamicSubCommandPath;

	private final TerminalMessage terminalMessage;

	public ExecActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path dynamicSubCommandPath, TerminalMessage terminalMessage) {
		this.templateEngine = templateEngine;
		this.model = model;
		this.dynamicSubCommandPath = dynamicSubCommandPath;
		this.terminalMessage = terminalMessage;
	}

	public void executeShellCommand(Exec exec, Map<String, Object> outputs) {
		if (!StringUtils.hasText(exec.getCommand()) && !StringUtils.hasText(exec.getCommandFile())) {
			throw new SpringCliException("No text found for command: or command-file: field in exec action.");
		}

		String commandToUse;
		if (StringUtils.hasText(exec.getCommand())) {
			commandToUse = templateEngine.process(exec.getCommand(), model);
		} else {
			String commandFileAsString = exec.getCommandFile();
			Path commandFilePath = Paths.get(String.valueOf(dynamicSubCommandPath), commandFileAsString);
			if (Files.exists(commandFilePath) && Files.isRegularFile(commandFilePath)) {
				try {
					List<String> lines = Files.readAllLines(commandFilePath);
					commandToUse = templateEngine.process(lines.get(0), model);
				}
				catch (IOException e) {
					throw new SpringCliException("Can not read from file " + commandFilePath.toAbsolutePath(), e);
				}
			} else {
				throw new SpringCliException("Can not read from file: " + commandFilePath.toAbsolutePath());
			}
		}

		String[] commands = {"bash", "-c", commandToUse};

		ProcessBuilder processBuilder = new ProcessBuilder(commands);
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
				String execGetTo = templateEngine.process(exec.getTo(), model);
				processBuilder.redirectOutput(new File(execGetTo));
			}
			catch (Exception e) {
				throw new SpringCliException("Error evaluating exec destination file. Expression: " + exec.getTo(),
						e);
			}
		}

		// If exec.getErrto() is set, the relative path to which to redirect stderr of the running process.
		if (exec.getErrto() != null) {
			try {
				String execErroTo = templateEngine.process(exec.getErrto(), model);
				processBuilder.redirectError(new File(execErroTo));
			}
			catch (Exception e) {
				throw new SpringCliException("Error evaluating exec error file. Expression: " + exec.getErrto(), e);
			}
		}

		try {
			terminalMessage.print("Executing: " + StringUtils.arrayToDelimitedString(commands, " ") );
			Process process = processBuilder.start();
			// capture the output.
			Optional<String> stderr = Optional.empty();
			Optional<String> stdout = Optional.empty();
			if (exec.getTo() == null) {
				stdout = readStringFromInputStream(process.getInputStream());
			}
			if (exec.getErrto() == null) {
				stderr = readStringFromInputStream(process.getErrorStream());
			}

			boolean exited = process.waitFor(300, TimeUnit.SECONDS);
			outputs.put(OUTPUT_EXIT_VALUE, process.exitValue());

			if (exec.getTo() == null && stdout.isPresent()) {
				outputs.put(OUTPUT_STDOUT, stdout.get());
			}
			if (exec.getErrto() == null && stderr.isPresent()) {
				outputs.put(OUTPUT_STDERR, stderr.get());
			}

			// Logging success or failure to terminal and optionally process stdout with JSON Path
			if (exited) {
				if (process.exitValue() == 0) {
					terminalMessage.print("Command executed successfully");
					if (exec.getJsonPath() != null) {
						Optional<Object> jsonPathOutput = applyJsonPath(exec, stdout);
						if (jsonPathOutput.isPresent()) {
							outputs.put(OUTPUT_STDOUT_JSONPATH, jsonPathOutput.get());
						}
					}
				}
				else {
					terminalMessage.print("Command '" + StringUtils.arrayToDelimitedString(commands, " ") + "' exited with value " + process.exitValue());
					if (stderr.isPresent()) {
						terminalMessage.print("stderr = " + stderr.get());
					}
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SpringCliException("Execution of command '"
					+ StringUtils.arrayToDelimitedString(commands, " ") + "' failed", e);

		}
		catch (IOException e) {
			throw new SpringCliException("Execution of command '"
					+ StringUtils.arrayToDelimitedString(commands, " ") + "' failed", e);
		}
	}

	private Optional<Object> applyJsonPath(Exec exec, Optional<String> stdout) {
		if (stdout.isPresent()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			mapper.registerModule(new JavaTimeModule());
			Object data = JsonPath.using(
					Configuration.builder()
							.jsonProvider(new JacksonJsonProvider(mapper))
							.mappingProvider(new JacksonMappingProvider(mapper))
							.build()
			).parse(stdout.get()).read(exec.getJsonPath());
			if (data != null) {
				return Optional.of(data);
			}
		}
		return Optional.empty();
	}

	private Optional<String> readStringFromInputStream(InputStream input) {
		final String newline = System.getProperty("line.separator");
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return Optional.of(buffer.lines().collect(Collectors.joining(newline)));
		}
		catch (IOException e) {
			logger.error("Could not read command output: " + e.getMessage());
		}
		return Optional.empty();
	}

}

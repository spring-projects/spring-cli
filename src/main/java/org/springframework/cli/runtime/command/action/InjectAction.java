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


package org.springframework.cli.runtime.command.action;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.frontmatter.CommandActionFileContents;
import org.springframework.cli.runtime.engine.frontmatter.Inject;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

/**
 * Handles the responsibility of injecting Strings into files.
 */
public class InjectAction {

	private static final Logger logger = LoggerFactory.getLogger(InjectAction.class);


	private TemplateEngine templateEngine;

	private Map<String, Object> model;

	private Path cwd;

	private TerminalMessage terminalMessage;

	public InjectAction(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
		this.templateEngine = templateEngine;
		this.model = model;
		this.cwd = cwd;
		this.terminalMessage = terminalMessage;
	}

	public void execute(Inject inject, CommandActionFileContents commandActionFileContents) {
		Path fileToInject = getFileToInject(inject, templateEngine, model, cwd);
		inject(fileToInject, commandActionFileContents, templateEngine, inject, model);
	}

	/**
	 *  Replaces any variables in the 'to:` field's value and check that the Path exists.
	 *  Returns a valid path to a file to inject.
	 */
	private Path getFileToInject(Inject inject, TemplateEngine templateEngine, Map<String, Object> model, Path cwd) {
		if (!StringUtils.hasText(inject.getTo())) {
			throw new SpringCliException("Inject action does not have a 'to:' field.");
		}
		String fileNameToInject = templateEngine.process(inject.getTo(), model);
		if (!StringUtils.hasText(fileNameToInject)) {
			throw new SpringCliException("Inject action can not be performed because the value of the 'to:' field resolved to an empty string.");
		}
		Path pathToFile = cwd.resolve(fileNameToInject).toAbsolutePath();
		if ((!pathToFile.toFile().exists())) {
			throw new SpringCliException("Inject action can not be performed because the file " + pathToFile + " does not exist.");
		}
		if ((pathToFile.toFile().isDirectory())) {
			throw new SpringCliException("Inject action can not be performed because the path " + pathToFile + " is a directory, not a file.");
		}
		return pathToFile;
	}

	private void inject(Path pathToFile, CommandActionFileContents commandActionFileContents, TemplateEngine templateEngine, Inject inject, Map<String, Object> model) {
		Path newFile = Paths.get(pathToFile + ".new");
		boolean shouldInject = shouldInjectFile(inject.getSkip(), pathToFile.toFile());
		if (!shouldInject) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(pathToFile);
			// process before injection
			if (StringUtils.hasText(inject.getBefore())) {
				int injectIndex = indexFromMarkerString(inject.getBefore(), lines);
				if (injectIndex != -1) {
					String result = templateEngine.process(commandActionFileContents.getText(), model);
					lines.add(injectIndex, result);
				}
				else {
					terminalMessage.print("Could not inject into file " + pathToFile.toFile().getAbsolutePath() + " no match on before: " + inject.getBefore());
				}
			}
			// process after injection
			if (StringUtils.hasText(inject.getAfter())) {
				int injectIndex = indexFromMarkerString(inject.getAfter(), lines);
				if (injectIndex != -1) {
					String result = templateEngine.process(commandActionFileContents.getText(), model);
					lines.add(injectIndex + 1, result);
				}
				else {
					terminalMessage.print("Could not inject into file " + pathToFile.toFile().getAbsolutePath() + " no match on after: " + inject.getAfter());
				}
			}

			// write updated file to with .new suffix.
			Files.write(newFile, lines, Charset.defaultCharset());
			// swap out files
			Files.copy(newFile, pathToFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
			// delete newFile
			deleteFile(newFile);
			terminalMessage.print("Injected into " + pathToFile.toFile().getAbsolutePath());
		}
		catch (IOException ex) {
			terminalMessage.print("Could not inject into file " + pathToFile.toFile().getAbsolutePath() + ".  Exception Message = " + ex.getMessage());
			deleteFile(newFile);
		}

	}

	private boolean shouldInjectFile(String skip, File file) {
		boolean shouldInject = true;
		try {
			String fileContents = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
			if (fileContents.contains(skip)) {
				terminalMessage.print("Skipping injection of " + file);
				shouldInject = false;
			}
		}
		catch (IOException e) {
			throw new SpringCliException("Could not read file contents of " + file.getAbsolutePath());
		}
		return shouldInject;
	}

	/**
	 * @param marker the string we are looking to insert before or after
	 * @param lines the lines of text
	 * @return the index to insert a new line, -1 if no match found.
	 */
	private int indexFromMarkerString(String marker, List<String> lines) {
		int i = 0;
		for (Iterator<String> it = lines.iterator(); it.hasNext(); i++) {
			String line = it.next();
			if (line.contains(marker)) {
				return i;
			}
		}
		if (i == lines.size()) {
			return -1;
		}
		else {
			return i;
		}
	}

	private void deleteFile(Path newFile) {
		if (Files.exists(newFile)) {
			try {
				Files.delete(newFile);
			}
			catch (IOException e) {
				logger.error("Could not delete file {}", newFile);
			}
		}
	}
}

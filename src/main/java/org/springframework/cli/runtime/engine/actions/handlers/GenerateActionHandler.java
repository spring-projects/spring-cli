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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.cli.runtime.engine.actions.Generate;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

public class GenerateActionHandler {
	private final TemplateEngine templateEngine;

	private final Map<String, Object> model;

	private final Path cwd;

	private final TerminalMessage terminalMessage;

	public GenerateActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
		this.templateEngine = templateEngine;
		this.model = model;
		this.cwd = cwd;
		this.terminalMessage = terminalMessage;
	}

	public void execute(Generate generate) {
		if (StringUtils.hasText(generate.getText())) {
			// This allows for variable replacement in the name of the generated file
			String toFileName = templateEngine.process(generate.getTo(), model);
			if (StringUtils.hasText(toFileName)) {
				try {
					generateFile(generate, templateEngine, toFileName, model, cwd);
				}
				catch (IOException e) {
					terminalMessage.print("Could not generate file " + toFileName);
					terminalMessage.print(e.getMessage());
				}
			}
		}
	}

	private void generateFile(Generate generate, TemplateEngine templateEngine, String toFileName, Map<String, Object> model, Path cwd) throws IOException {
		Path pathToFile = cwd.resolve(toFileName).toAbsolutePath();
		if ((pathToFile.toFile().exists() && generate.isOverwrite()) || (!pathToFile.toFile().exists())) {
			writeFile(generate, templateEngine, model, pathToFile);
		}
		else {
			terminalMessage.print("Skipping generation of " + pathToFile + ".  File exists and overwrite option not specified.");
		}
	}


	private void writeFile(Generate generate, TemplateEngine templateEngine,
			Map<String, Object> model, Path pathToFile) throws IOException {
		Files.createDirectories(pathToFile.getParent());
		String result = templateEngine.process(generate.getText(), model);
		Files.write(pathToFile, result.getBytes());
		// TODO: keep log of action taken so can report later.
		terminalMessage.print("Generated "+ pathToFile);
	}

}

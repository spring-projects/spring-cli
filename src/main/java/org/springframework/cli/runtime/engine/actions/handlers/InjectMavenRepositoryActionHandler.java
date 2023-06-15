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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openrewrite.Result;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.xml.tree.Xml.Document;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.recipe.InjectTextMavenRepositoryRecipe;
import org.springframework.cli.runtime.engine.actions.InjectMavenRepository;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenRepositoryReader;
import org.springframework.cli.util.TerminalMessage;

public class InjectMavenRepositoryActionHandler extends AbstractInjectMavenActionHandler {

	public InjectMavenRepositoryActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
		super(templateEngine, model, cwd, terminalMessage);
	}

	public void execute(InjectMavenRepository injectMavenRepository) {
		Path pomPath = getPomPath();
		String text = getTextToUse(injectMavenRepository.getText(), "Inject Maven Repository");
		MavenRepositoryReader mavenRepositoryReader = new MavenRepositoryReader();
		String[] mavenRepositories = mavenRepositoryReader.parseMavenRepositories(text);
		for (String mavenRepository : mavenRepositories) {


			InjectTextMavenRepositoryRecipe injectTextMavenRepositoryRecipe = new InjectTextMavenRepositoryRecipe(mavenRepository);
			List<Path> paths = new ArrayList<>();
			paths.add(pomPath);
			MavenParser mavenParser = MavenParser.builder().build();
			List<Document> parsedPomFiles = mavenParser.parse(paths, cwd, getExecutionContext());
			List<Result> resultList = injectTextMavenRepositoryRecipe.run(parsedPomFiles).getResults();
			try {
				for (Result result : resultList) {
					// write updated file.
					try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(pomPath, StandardCharsets.UTF_8)) {
						sourceFileWriter.write(result.getAfter().printAllTrimmed());
					}
				}
			}
			catch (IOException ex) {
				throw new SpringCliException("Error writing to " + pomPath.toAbsolutePath(), ex);
			}
		}
	}
}

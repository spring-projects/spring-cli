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

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.maven.MavenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractInjectMavenActionHandler {
	private static final Logger logger = LoggerFactory.getLogger(InjectMavenDependencyActionHandler.class);

	protected final TemplateEngine templateEngine;

	protected final Map<String, Object> model;

	protected final Path cwd;

	protected final TerminalMessage terminalMessage;

	public AbstractInjectMavenActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
		this.templateEngine = templateEngine;
		this.model = model;
		this.cwd = cwd;
		this.terminalMessage = terminalMessage;
	}

	static protected ExecutionContext getExecutionContext() {
		Consumer<Throwable> onError = e -> {
			logger.error("error in javaParser execution", e);
		};
		return new InMemoryExecutionContext(onError);
	}

	protected String getTextToUse(String text, String actionName) {
		if (!StringUtils.hasText(text)) {
			throw new SpringCliException(actionName + " action does not have a value in the 'text:' field.");
		}
		if (this.templateEngine != null) {
			text = this.templateEngine.process(text, model);
		}
		return text;
	}

	@NotNull
	protected Path getPomPath() {
		Path pomPath = cwd.resolve("pom.xml");
		if (Files.notExists(pomPath)) {
			throw new SpringCliException("Could not find pom.xml in " + this.cwd + ".  Make sure you are running the command in the directory that contains a pom.xml file");
		}
		return pomPath;
	}

	protected void execRecipe(Path pomPath, Recipe recipe) {
		List<Result> resultList = runRecipe(pomPath, List.of(recipe), cwd).getChangeset().getAllResults();
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

	static public RecipeRun runRecipe(Path pomPath, List<Recipe> recipe, Path cwd) {
		List<Path> paths = new ArrayList<>();
		paths.add(pomPath);
		DeclarativeRecipe aggregateRecipe = new DeclarativeRecipe("spring.cli.ai.AddDependencies", "Add Pom changes from AI", "", Collections.emptySet(), null, null, false, Collections.emptyList());		MavenParser mavenParser = MavenParser.builder().build();
		aggregateRecipe.getRecipeList().addAll(recipe);
		List<SourceFile> parsedPomFiles = mavenParser.parse(paths, cwd, getExecutionContext()).toList();
		return aggregateRecipe.run(new InMemoryLargeSourceSet(parsedPomFiles), getExecutionContext());
	}

}

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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openrewrite.Recipe;
import org.springframework.cli.recipe.AddManagedDependencyRecipeFactory;
import org.springframework.cli.recipe.InjectTextMavenRepositoryRecipe;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependencyManagement;
import org.springframework.cli.runtime.engine.actions.InjectMavenRepository;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenDependencyReader;
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
		String[] mavenRepositories = mavenRepositoryReader.parseMavenSection(text);
		for (String mavenRepository : mavenRepositories) {
			InjectTextMavenRepositoryRecipe injectTextMavenRepositoryRecipe = new InjectTextMavenRepositoryRecipe(mavenRepository);
			execRecipe(pomPath, injectTextMavenRepositoryRecipe);
		}
	}

	public List<Recipe> getRecipe(InjectMavenRepository injectMavenRepository) {
		String text = getTextToUse(injectMavenRepository.getText(), "Inject Maven Repository");
		MavenRepositoryReader mavenRepositoryReader = new MavenRepositoryReader();
		String[] mavenRepositories = mavenRepositoryReader.parseMavenSection(text);
		return Arrays.stream(mavenRepositories).map(InjectTextMavenRepositoryRecipe::new).collect(Collectors.toList());
	}

}

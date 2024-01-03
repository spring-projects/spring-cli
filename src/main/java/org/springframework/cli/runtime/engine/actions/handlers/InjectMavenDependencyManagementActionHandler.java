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

import org.openrewrite.maven.AddManagedDependency;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.recipe.AddManagedDependencyRecipeFactory;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependencyManagement;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenDependencyReader;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.Map;

public class InjectMavenDependencyManagementActionHandler extends AbstractInjectMavenActionHandler {

	public InjectMavenDependencyManagementActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
		super(templateEngine, model, cwd, terminalMessage);
	}

	public void execute(InjectMavenDependencyManagement injectMavenDependencyManagement) {
		Path pomPath = getPomPath();
		String text = getTextToUse(injectMavenDependencyManagement.getText(), "Inject Maven Dependency Management");
		if (!StringUtils.hasText(text)) {
			throw new SpringCliException("Inject Maven Dependency Management action does not have a value in the 'text:' field.");
		}

		MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
		String[] mavenDependencies = mavenDependencyReader.parseMavenSection(text);
		for (String mavenDependency : mavenDependencies) {
			AddManagedDependencyRecipeFactory recipeFactory = new AddManagedDependencyRecipeFactory();
			AddManagedDependency addManagedDependency = recipeFactory.create(mavenDependency);
  			runRecipe(pomPath, addManagedDependency);
		}
	}
}

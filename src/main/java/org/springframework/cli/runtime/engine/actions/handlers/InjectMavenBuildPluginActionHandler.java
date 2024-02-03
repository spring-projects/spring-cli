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

import org.openrewrite.Recipe;
import org.openrewrite.maven.AddPlugin;
import org.springframework.cli.recipe.AddPluginRecipeFactory;
import org.springframework.cli.runtime.engine.actions.InjectMavenBuildPlugin;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenBuildPluginReader;
import org.springframework.cli.util.TerminalMessage;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InjectMavenBuildPluginActionHandler extends AbstractInjectMavenActionHandler {

	public InjectMavenBuildPluginActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
		super(templateEngine, model, cwd, terminalMessage);
	}

	public void execute(InjectMavenBuildPlugin injectMavenBuildPlugin) {
		Path pomPath = getPomPath();
		String text = getTextToUse(injectMavenBuildPlugin.getText(), "Inject Maven Build Plugin");
		MavenBuildPluginReader mavenBuildPluginReader = new MavenBuildPluginReader();
		String[] buildPlugins = mavenBuildPluginReader.parseMavenSection(text);
		for (String buildPlugin : buildPlugins) {
			AddPluginRecipeFactory recipeFactory = new AddPluginRecipeFactory();
			AddPlugin addPlugin = recipeFactory.create(buildPlugin);
			execRecipe(pomPath, addPlugin);
		}
	}

	public List<Recipe> getRecipe(InjectMavenBuildPlugin injectMavenBuildPlugin) {
		String text = getTextToUse(injectMavenBuildPlugin.getText(), "Inject Maven Build Plugin");
		MavenBuildPluginReader mavenBuildPluginReader = new MavenBuildPluginReader();
		String[] buildPlugins = mavenBuildPluginReader.parseMavenSection(text);
		return Arrays.stream(buildPlugins).map(buildPlugin -> new AddPluginRecipeFactory().create(buildPlugin)).collect(Collectors.toList());
	}

}

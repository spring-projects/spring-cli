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
import org.openrewrite.RecipeRun;
import org.openrewrite.config.DeclarativeRecipe;
import org.springframework.cli.recipe.AddDependencyRecipeFactory;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependency;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenDependencyReader;
import org.springframework.cli.util.TerminalMessage;

import java.util.List;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class InjectMavenDependencyActionHandler extends AbstractInjectMavenActionHandler {

    public InjectMavenDependencyActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
        super(templateEngine, model, cwd, terminalMessage);
    }

    public void execute(InjectMavenDependency injectMavenDependency) {
        String text = getTextToUse(injectMavenDependency.getText(), "Inject Maven Dependency");
        MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
        String[] mavenDependencies = mavenDependencyReader.parseMavenSection(text);
        for (String mavenDependency : mavenDependencies) {
            AddDependencyRecipeFactory recipeFactory = new AddDependencyRecipeFactory();
            Recipe addDependency = recipeFactory.create(mavenDependency);
            Path pomPath = getPomPath();
            execRecipe(pomPath, addDependency);
        }
    }

    public List<Recipe> getRecipe(InjectMavenDependency injectMavenDependency) {
        String text = getTextToUse(injectMavenDependency.getText(), "Inject Maven Dependency");
        MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
        String[] mavenDependencies = mavenDependencyReader.parseMavenSection(text);
        return Arrays.stream(mavenDependencies).map(mavenDependency -> new AddDependencyRecipeFactory().create(mavenDependency)).collect(Collectors.toList());
    }

}

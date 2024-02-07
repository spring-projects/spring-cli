/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.maven.MavenParser;
import org.springframework.cli.recipe.AddDependencyRecipeFactory;
import org.springframework.cli.recipe.AddManagedDependencyRecipeFactory;
import org.springframework.cli.recipe.AddPluginRecipeFactory;
import org.springframework.cli.recipe.InjectTextMavenRepositoryRecipe;
import org.springframework.cli.runtime.engine.actions.InjectMavenBuildPlugin;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependency;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependencyManagement;
import org.springframework.cli.runtime.engine.actions.InjectMavenRepository;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenBuildPluginReader;
import org.springframework.cli.util.MavenDependencyReader;
import org.springframework.cli.util.MavenRepositoryReader;
import org.springframework.cli.util.TerminalMessage;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class InjectMavenActionHandler extends AbstractInjectMavenActionHandler {

    private List<InjectMavenDependency> dependencies;
    private List<InjectMavenBuildPlugin> buildPlugins;

    private List<InjectMavenRepository> repositories;

    private List<InjectMavenDependencyManagement> dependencyManagements;

    public InjectMavenActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
        super(templateEngine, model, cwd, terminalMessage);
        this.dependencies = new ArrayList<>();
        this.buildPlugins = new ArrayList<>();
        this.repositories = new ArrayList<>();
        this.dependencyManagements = new ArrayList<>();
    }

    public boolean injectBuildPlugin(InjectMavenBuildPlugin buildPlugin) {
        return buildPlugins.add(buildPlugin);
    }

    public boolean injectDependency(InjectMavenDependency dependency) {
        return dependencies.add(dependency);
    }

    public boolean injectRepository(InjectMavenRepository repository) {
        return repositories.add(repository);
    }

    public boolean injectDependencyManagement(InjectMavenDependencyManagement dependencyManagement) {
        return dependencyManagements.add(dependencyManagement);
    }

    protected Recipe createRecipe() {
        DeclarativeRecipe aggregateRecipe = new DeclarativeRecipe("spring.cli.ai.MavenUpdates", "Add Pom changes from AI", "", Collections.emptySet(), null, null, false, Collections.emptyList());		MavenParser mavenParser = MavenParser.builder().build();
        for (InjectMavenDependency d : dependencies) {
            String text = getTextToUse(d.getText(), "Inject Maven Dependency");
            MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
            String[] mavenDependencies = mavenDependencyReader.parseMavenSection(text);
            for (String md : mavenDependencies) {
                aggregateRecipe.getRecipeList().add(new AddDependencyRecipeFactory().create(md));
            }
        }
        for (InjectMavenBuildPlugin p : buildPlugins) {
            String text = getTextToUse(p.getText(), "Inject Maven Build Plugin");
            MavenBuildPluginReader mavenBuildPluginReader = new MavenBuildPluginReader();
            String[] buildPlugins = mavenBuildPluginReader.parseMavenSection(text);
            for (String mp : buildPlugins) {
                aggregateRecipe.getRecipeList().add(new AddPluginRecipeFactory().create(mp));
            }
        }
        for (InjectMavenRepository r : repositories) {
            String text = getTextToUse(r.getText(), "Inject Maven Repository");
            MavenRepositoryReader mavenRepositoryReader = new MavenRepositoryReader();
            String[] mavenRepositories = mavenRepositoryReader.parseMavenSection(text);
            for (String mr : mavenRepositories) {
                aggregateRecipe.getRecipeList().add(new InjectTextMavenRepositoryRecipe(mr));
            }
        }
        for (InjectMavenDependencyManagement dm : dependencyManagements) {
            String text = getTextToUse(dm.getText(), "Inject Maven Dependency Management");
            MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
            String[] mavenDependencyManagements = mavenDependencyReader.parseMavenSection(text);
            for (String mdm : mavenDependencyManagements) {
                aggregateRecipe.getRecipeList().add( new AddManagedDependencyRecipeFactory().create(mdm));
            }
        }
        return aggregateRecipe;
    }

}

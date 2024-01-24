/*
 * Copyright 2021 - 2023 the original author or authors.
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
package org.springframework.cli.recipe;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.rewrite.parsers.RewriteProjectParser;
import org.springframework.rewrite.parsers.RewriteProjectParsingResult;
import org.springframework.rewrite.project.resource.ProjectResourceSet;
import org.springframework.rewrite.project.resource.ProjectResourceSetFactory;
import org.springframework.rewrite.project.resource.ProjectResourceSetSerializer;
import org.springframework.rewrite.recipes.RewriteRecipeDiscovery;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Fabian Krüger
 */
@Component
public class RewriteRecipeLauncher {

    private static final Logger logger = LoggerFactory.getLogger(RewriteRecipeLauncher.class);

    private final RewriteProjectParser parser;
    private final RewriteRecipeDiscovery discovery;
    private final ProjectResourceSetFactory resourceSetFactory;
    private final ProjectResourceSetSerializer serializer;

    public interface RewriteRecipeRunnerProgressListener {
        void publish(String progressMessage);
    }

    public RewriteRecipeLauncher(RewriteProjectParser parser, RewriteRecipeDiscovery discovery, ProjectResourceSetFactory resourceSetFactory, ProjectResourceSetSerializer serializer) {
        this.parser = parser;
        this.discovery = discovery;
        this.resourceSetFactory = resourceSetFactory;
        this.serializer = serializer;
    }

    public void run(String recipeName, String path) {
        run(recipeName, path, __ -> {});
    }

    public void run(String recipeName, String path, RewriteRecipeRunnerProgressListener listener) {
        Path baseDir = getBaseDir(path);
        RewriteProjectParsingResult parsingResult = parseProject(baseDir, listener);
        Optional<Recipe> recipe = discoverRecipe(recipeName);
        if (recipe.isPresent()) {
            applyRecipe(recipeName, baseDir, parsingResult, recipe, listener);
        } else {
            logger.error("Could not find recipe " + recipeName + ".");
        }
    }

    @NotNull
    private RewriteProjectParsingResult parseProject(Path baseDir, RewriteRecipeRunnerProgressListener listener) {
        listener.publish("Start parsing dir '%s'".formatted(baseDir));
        StopWatch stopWatch = new StopWatch("parse");
        stopWatch.start();
        RewriteProjectParsingResult parsingResult = parser.parse(baseDir);
        stopWatch.stop();
        double parseTime = stopWatch.getTotalTime(TimeUnit.SECONDS);
        listener.publish("Parsed %d resources in %f sec.".formatted(parsingResult.sourceFiles().size(), parseTime));
        return parsingResult;
    }

    private void applyRecipe(String recipeName, Path baseDir, RewriteProjectParsingResult parsingResult, Optional<Recipe> recipe, RewriteRecipeRunnerProgressListener listener) {
        StopWatch stopWatch = new StopWatch("parse");
        stopWatch.start();
        // Use ProjectResourceSet abstraction
        ProjectResourceSet projectResourceSet = resourceSetFactory.create(baseDir, parsingResult.sourceFiles());
        // To apply recipes
        listener.publish("Applying recipe %s, this may take a few minutes.".formatted(recipeName));
        projectResourceSet.apply(recipe.get());
        stopWatch.stop();
        double recipeRunTime = stopWatch.getTotalTime(TimeUnit.MINUTES);
        listener.publish("Applied recipe %s in %f min.".formatted(recipeName, recipeRunTime));
        // Synchronize changes with filesystem
        listener.publish("Write changes from %s.".formatted(recipeName));
        serializer.writeChanges(projectResourceSet);
    }

    @NotNull
    private Optional<Recipe> discoverRecipe(String recipeName) {
        // discover recipe
        List<Recipe> recipes = discovery.discoverRecipes();
        Optional<Recipe> recipe = recipes.stream().filter(r -> recipeName.equals(r.getName())).findFirst();
        return recipe;
    }

    @NotNull
    private static Path getBaseDir(String path) {
        Path baseDir = Path.of(".").toAbsolutePath().normalize();
        if(path != null) {
            baseDir = Path.of(path).toAbsolutePath().normalize();
        }
        return baseDir;
    }
}

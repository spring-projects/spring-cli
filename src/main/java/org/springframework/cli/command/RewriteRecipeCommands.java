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
package org.springframework.cli.command;

import org.jetbrains.annotations.ApiStatus;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cli.command.recipe.RecipeLoadingException;
import org.springframework.cli.command.recipe.catalog.RecipeParameterDefinitions;
import org.springframework.cli.command.recipe.RecipeParameterParser;
import org.springframework.cli.command.recipe.catalog.RecipeCatalog;
import org.springframework.cli.command.recipe.catalog.RecipeCatalogEntry;
import org.springframework.rewrite.RewriteRecipeLauncher;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Fabian Krüger
 */
@Command(command = "recipe-catalog", group = "OpenRewrite")
public class RewriteRecipeCommands {
    private static final Logger logger = LoggerFactory.getLogger(RewriteRecipeCommands.class);

    private final RewriteRecipeLauncher rewriteRecipeLauncher;
    private final RecipeCatalog recipeCatalog;
    private final RecipeParameterParser recipeParameterParser;

    public RewriteRecipeCommands(RewriteRecipeLauncher rewriteRecipeLauncher, RecipeCatalog recipeCatalog, RecipeParameterParser recipeParameterParser) {
        this.rewriteRecipeLauncher = rewriteRecipeLauncher;
        this.recipeCatalog = recipeCatalog;
        this.recipeParameterParser = recipeParameterParser;
    }

    @Command(command = "apply",
            description = "Apply an OpenRewrite recipe.")
    public void apply(
            @Option(description = "The recipeId as declared in the recipe catalog.") String recipeId,
            @Option(label = "--parameters", description = "the parameters for the recipe (see docs)", defaultValue = ShellOption.NULL) String parameter,
            @Option(description = "The application path") String path) {

        Optional<RecipeCatalogEntry> recipeDataFound = recipeCatalog.findRecipeData(recipeId);
        RecipeCatalogEntry catalogEntry = recipeDataFound.orElseThrow(() -> new IllegalArgumentException("Could not find recipe for id '%s' in recipe catalog.".formatted(recipeId)));
        RecipeParameterDefinitions parameterDefinitions = catalogEntry.getParameterDefinitions();
        Map<String, List<String>> parameterMap = recipeParameterParser.parseToListMap(parameter, parameterDefinitions);
        try {
            Optional<Recipe> recipeFound = recipeCatalog.lookup(recipeId, parameterMap);
            if (recipeFound.isPresent()) {
                Recipe recipe = recipeFound.get();
                rewriteRecipeLauncher.run(recipe, path);
            }
        } catch (RecipeLoadingException e) {
            logger.error(e.getMessage());
        }
    }

}

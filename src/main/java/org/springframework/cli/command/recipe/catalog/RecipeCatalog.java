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
package org.springframework.cli.command.recipe.catalog;

import org.openrewrite.Recipe;
import org.springframework.cli.command.recipe.RecipeClassLoader;
import org.springframework.cli.command.recipe.RecipeLoadingException;
import org.springframework.cli.command.recipe.resolver.MavenArtifactDownloader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Fabian Krüger
 */
@Component
public class RecipeCatalog {

    private final RecipeCatalogReader recipeCatalogReader;
    private final MavenArtifactDownloader mavenArtifactDownloader;

    public RecipeCatalog(MavenArtifactDownloader mavenArtifactDownloader) {
        this.mavenArtifactDownloader = mavenArtifactDownloader;
        this.recipeCatalogReader = new RecipeCatalogReader();
    }

    public Optional<Recipe> lookup(String recipeId, Map<String, List<String>> parameters) throws RecipeLoadingException {
        Optional<RecipeCatalogEntry> catalogEntryFound = getRecipeData(recipeId);
        if (catalogEntryFound.isEmpty()) {
            return Optional.empty();
        }
        RecipeCatalogEntry catalogEntry = catalogEntryFound.get();
        String gav = catalogEntry.getArtifactGav();
        Set<Path> recipeJars = mavenArtifactDownloader.downloadRecipeJars(gav);
        Recipe recipe = loadRecipe(catalogEntry.getRecipeName(), recipeJars, parameters);
        return Optional.ofNullable(recipe);
    }

    private Optional<RecipeCatalogEntry> getRecipeData(String recipeId) {
        return findRecipeData(recipeId);
    }

    public Optional<RecipeCatalogEntry> findRecipeData(String recipeId) {
        InputStream inputStream = getRecipeInputStream();
        RecipeCatalogEntries recipeCatalogEntries = recipeCatalogReader.loadCatalogData(inputStream);
        RecipeCatalogEntry catalogEntry = recipeCatalogEntries.byName(recipeId);
        return Optional.ofNullable(catalogEntry);
    }

    private static InputStream getRecipeInputStream() {
        try {
            return new ClassPathResource("org/springframework/cli/recipes/recipe-catalog.yaml").getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Recipe loadRecipe(String recipeName, Set<Path> recipeJars, Map<String, List<String>> parameters) throws RecipeLoadingException {
        Class<?> recipeClazz = RecipeClassLoader.load(recipeName, recipeJars);

        Constructor<?> recipeConstructor = Arrays.stream(recipeClazz.getConstructors())
                .filter(c -> c.getParameterCount() == parameters.size())
                .filter(c -> Arrays.stream(c.getParameters())
                        .allMatch(p -> parameters.keySet().contains(p.getName())))
                .findFirst()
                .orElseThrow(() -> new RecipeLoadingException("Could not find matching constructor for recipe: %s with parameters: %s".formatted(recipeName, parameters)));

        Object[] params = Arrays.stream(recipeConstructor.getParameters())
                .map(p -> mapParameter(parameters, p))
                .toArray(Object[]::new);
        Object newInstance = createNewInstance(recipeConstructor, params);
        if (!Recipe.class.isInstance(newInstance)) {
            throw new RecipeLoadingException("Class %s is not of type %.".formatted(newInstance.getClass().getName(), Recipe.class.getName()));
        }
        Recipe recipe = Recipe.class.cast(newInstance);
        return recipe;
    }

    private static Object createNewInstance(Constructor<?> recipeConstructor, Object[] params) throws RecipeLoadingException {
        try {
            return recipeConstructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RecipeLoadingException("Could not create instance using constructor '%s#%s(%s)'".formatted(recipeConstructor.getDeclaringClass().getName(), recipeConstructor.getName(), recipeConstructor.getParameterTypes()), e);
        }
    }

    private static Object mapParameter(Map<String, List<String>> parameters, Parameter p) {
        if (String.class == p.getType()) {
            String parameterName = p.getName();
            return new String(parameters.get(parameterName).get(0));
        }
        throw new IllegalArgumentException("Cannot handle parameter of type %s".formatted(p.getType().getName()));
    }
}

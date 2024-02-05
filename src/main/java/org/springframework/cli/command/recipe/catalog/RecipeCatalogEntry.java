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

//        recipe-catalog
//                - name: "junit"
//        description: "Upgrade from JUnit 4 to JUnit 5"
//        url: "maven://org.openrewrite.recipe:rewrite-testing-frameworks:2.3.1
//        class: org.openrewrite.java.testing.junit5.JUnit4to5Migration
//        tags:
//        - "testing"
//                - "junit"

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabian Krüger
 */
public class RecipeCatalogEntry {
    /**
     * Name of the catalog entry.
     */
    private String recipeId;
    private String description;

    private String repositoryUrl;
    /**
     * GAV of the recipe.
     */
    private String artifactGav;
    /**
     * The OpenRewrite recipe name.
     */
    private String recipeName;

    private List<RecipeParameterDefinition> parameters = new ArrayList<>();

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getArtifactGav() {
        return artifactGav;
    }

    public void setArtifactGav(String artifactGav) {
        this.artifactGav = artifactGav;
    }

    public List<RecipeParameterDefinition> getParameters() {
        return parameters;
    }

    public void setParameters(List<RecipeParameterDefinition> parameters) {
        this.parameters = parameters;
    }

    public RecipeParameterDefinitions getParameterDefinitions() {
        return new RecipeParameterDefinitions(parameters);
    }
}

/*
 * Copyright 2024. the original author or authors.
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.AddLicenseHeader;
import org.springframework.cli.command.recipe.RecipeLoadingException;
import org.springframework.cli.command.recipe.resolver.MavenArtifactDownloader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Fabian Krüger
 */
class RecipeCatalogTests {
    @Test
    @DisplayName("should lookup AddLicenseHeader recipe in catalog")
    void lookupAddLicenseHeaderRecipe() throws RecipeLoadingException {
        RecipeCatalog sut = new RecipeCatalog(new MavenArtifactDownloader());
        Optional<Recipe> recipeFound = sut.lookup("AddLicenseHeader", Map.of("licenseText", List.of("my license")));
        assertThat(recipeFound).isPresent();
        Recipe recipe = recipeFound.get();
        assertThat(recipe.getName()).isEqualTo("org.openrewrite.java.AddLicenseHeader");
        assertThat(recipe).isInstanceOf(AddLicenseHeader.class);
        AddLicenseHeader addLicenseHeader = AddLicenseHeader.class.cast(recipe);
        assertThat(addLicenseHeader.getLicenseText()).isEqualTo("my license");
    }
    
    @Test
    @DisplayName("recipe not found")
    void recipeNotFound() {
        assertThatThrownBy(() -> {
            RecipeCatalog sut = new RecipeCatalog(new MavenArtifactDownloader());
            sut.lookup("NoRecipeWithThisName", Map.of());
        }).isInstanceOf(IllegalArgumentException.class);
    }
}
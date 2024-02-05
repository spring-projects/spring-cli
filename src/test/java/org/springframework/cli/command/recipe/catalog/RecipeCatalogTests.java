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
package org.springframework.cli.command.recipe.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
class RecipeCatalogReaderTest {

    @Test
    @DisplayName("should read Catalog")
    void shouldReadCatalog()  {

        String catalogYaml = """
                            recipeCatalog:
                              - recipeId: the-recipe-id
                                artifactGav: com:foo:bar:1.0.0
                                recipeName: AddLicenseHeaders
                                description: Some description
                                repositoryUrl: https://github.com/company/repo
                                parameters:
                                  - name: licenseText
                                    type: String
                            """;
        RecipeCatalogReader recipeCatalogReader = new RecipeCatalogReader();
        RecipeCatalogEntries recipeCatalogEntries = recipeCatalogReader.loadCatalogData(new ByteArrayInputStream(catalogYaml.getBytes()));
        assertThat(recipeCatalogEntries.getCatalogEntries()).hasSize(1);
        assertThat(recipeCatalogEntries.getCatalogEntries().get(0).getRecipeId()).isEqualTo("the-recipe-id");
    }
}
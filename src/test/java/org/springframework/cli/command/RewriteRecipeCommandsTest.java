package org.springframework.cli.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openrewrite.Recipe;
import org.openrewrite.java.AddLicenseHeader;
import org.springframework.cli.command.recipe.RecipeLoadingException;
import org.springframework.cli.command.recipe.catalog.RecipeParameterDefinitions;
import org.springframework.cli.command.recipe.catalog.RecipeParameterDefinition;
import org.springframework.cli.command.recipe.RecipeParameterParser;
import org.springframework.cli.command.recipe.catalog.RecipeCatalog;
import org.springframework.cli.command.recipe.catalog.RecipeCatalogEntry;
import org.springframework.rewrite.RewriteRecipeLauncher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Fabian Krüger
 */
@ExtendWith(MockitoExtension.class)
class RewriteRecipeCommandsTest {

    @Mock
    private RewriteRecipeLauncher rewriteRecipeLauncher;
    @Mock
    private RecipeParameterParser recipeParameterParser;
    @Mock
    RecipeCatalog recipeCatalog;

    @InjectMocks
    private RewriteRecipeCommands sut;

    @Test
    @DisplayName("applyByName")
    void applyByName() throws RecipeLoadingException {
        String path = "some/path";
        String parameter = "parameter=value";
        Map<String, List<String>> parameterMap = Map.of();
        String recipeId = "recipe-id";
        Recipe recipe = new AddLicenseHeader("");

        RecipeCatalogEntry catalogEntry = new RecipeCatalogEntry();
        RecipeParameterDefinition recipeParameterDefinition = new RecipeParameterDefinition();
        recipeParameterDefinition.setName("parameter");
        recipeParameterDefinition.setRequired(true);
        List<RecipeParameterDefinition> recipeParameterDefinitions = List.of(recipeParameterDefinition);
        catalogEntry.setParameters(recipeParameterDefinitions);
        RecipeParameterDefinitions parameterDefinitions = new RecipeParameterDefinitions(recipeParameterDefinitions);
        Optional<RecipeCatalogEntry> recipeDataFound = Optional.of(catalogEntry);

        when(recipeCatalog.findRecipeData(recipeId)).thenReturn(recipeDataFound);
        when(recipeParameterParser.parseToListMap(eq(parameter), eq(parameterDefinitions))).thenReturn(parameterMap);
        when(recipeCatalog.lookup(recipeId, parameterMap)).thenReturn(Optional.of(recipe));

        sut.apply(recipeId, parameter, path);
        verify(rewriteRecipeLauncher).run(recipe, path);
    }
}
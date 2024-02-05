package org.springframework.cli.command.recipe.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Fabian Krüger
 */
class RecipeParameterParserTest {
    @Test
    @DisplayName("should parse single String parameter")
    void shouldParseSingleStringParameter() {
        String parameter = "the-string-param=\"some string^\"";
    }
}
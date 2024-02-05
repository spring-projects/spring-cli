package org.springframework.cli.command.recipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cli.command.recipe.catalog.RecipeParameterDefinitions;
import org.springframework.cli.command.recipe.catalog.RecipeParameterDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
class RecipeParameterParserTest {

    private RecipeParameterParser sut = new RecipeParameterParser();

    @Test
    @DisplayName("should parse String parameter")
    void shouldParseStringParameter() {
        String parameter = "param=\"a String\"";
        RecipeParameterDefinition recipeParameterDefinition = new RecipeParameterDefinition();
        recipeParameterDefinition.setRequired(true);
        recipeParameterDefinition.setName("param");
        RecipeParameterDefinitions recipeParameterDefinitions = new RecipeParameterDefinitions(List.of(recipeParameterDefinition));
        Map<String, List<String>> parse = sut.parseToListMap(parameter, recipeParameterDefinitions);
        assertThat(parse.get("param")).containsExactly("a String");
    }
    
    @Test
    @DisplayName("should parse parameter names")
    void shouldParseParameterNames() {
        String parameterPair = " paramName = paramValue ; param2=[a=b,c=d]";
        Set<String> parameterNames = sut.parseParameterNames(parameterPair);
        assertThat(parameterNames).containsExactly("paramName", "param2");
    }
}
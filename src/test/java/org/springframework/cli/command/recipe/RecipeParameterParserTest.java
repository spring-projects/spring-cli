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
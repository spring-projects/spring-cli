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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Fabian Krüger
 */
public class RecipeParameterDefinitions {
    private final List<RecipeParameterDefinition> parameters;

    public RecipeParameterDefinitions(List<RecipeParameterDefinition> parameters) {
        this.parameters = parameters;
    }

    public Stream<RecipeParameterDefinition> stream() {
        return parameters.stream();
    }

    public Set<String> getRequiredParameterNames() {
       return stream().filter(p -> p.isRequired()).map(RecipeParameterDefinition::getName).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeParameterDefinitions that = (RecipeParameterDefinitions) o;
        return Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }
}

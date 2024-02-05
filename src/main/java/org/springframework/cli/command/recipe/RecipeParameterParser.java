/*
 * Copyright 2017-2022 the original author or authors.
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

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cli.command.recipe.catalog.RecipeParameterDefinitions;


/**
 * Provides utility methods for formatting and parsing deployment properties.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public final class RecipeParameterParser {
    private static final Logger logger = LoggerFactory.getLogger(RecipeParameterParser.class);
    private static final String parameterSeparator = ";";
    public RecipeParameterParser() {
        // prevent instantiation
    }

    public Map<String, List<String>> parseToListMap(String parameterString, RecipeParameterDefinitions parameterDefinitions) {

        Set<String> providedParameterNames = parseParameterNames(parameterString);
        Set<String> requiredParameterNames = parameterDefinitions.getRequiredParameterNames();
        if(!providedParameterNames.containsAll(requiredParameterNames)) {
            Set<String> missingParameterNames = requiredParameterNames;
            missingParameterNames.removeAll(providedParameterNames);
            throw new IllegalArgumentException("Missing required parameters: %s".formatted(missingParameterNames));
        }

        return Arrays.stream(parameterString.split(parameterSeparator))
                .map(this::mapTpParameterPair)
                .collect(Collectors.toMap(
                        pp -> pp.parameterName(),
                        pp -> List.of(pp.parameterValue())
                ));
    }

    public Set<String> parseParameterNames(String parameterString) {
        return Arrays.stream(parameterString.split(parameterSeparator))
                .map(this::mapTpParameterPair)
                .map(ParameterPair::parameterName)
                .collect(Collectors.toSet());
    }

    private ParameterPair mapTpParameterPair(String parameterPair) {
        int pos = parameterPair.indexOf("=");
        String parameterName = parameterPair.substring(0, pos);
        String parameterValue = parameterPair.substring(pos+1);
        return new ParameterPair(parameterName, parameterValue);
    }

    private record ParameterPair(String parameterName, String parameterValue) {

        private ParameterPair(String parameterName, String parameterValue) {
            this.parameterName = parameterName.trim().replaceAll("^\"|\"$", "");
            this.parameterValue = parameterValue.trim().replaceAll("^\"|\"$", "");
        }
    }
}

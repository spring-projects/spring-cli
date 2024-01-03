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
package org.springframework.cli.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddPlugin;

/**
 * @author Fabian Krüger
 */
public class AddPluginRecipeFactory {

    /**
     * Create {@link AddPlugin} from Maven plugin XML snippet.
     */
    public AddPlugin create(String buildPlugin) {
        try {
            ObjectMapper om = new XmlMapper();
            JsonNode jsonNode = om.readTree(buildPlugin);
            String groupId = jsonNode.get("groupId").textValue();
            String artifactId = jsonNode.get("artifactId").textValue();
            @Nullable String version = getNullable(jsonNode, "version");
            @Nullable String configuration = getNullable(jsonNode, "configuration");
            @Nullable String dependencies = getNullable(jsonNode, "dependencies");
            @Nullable String executions = getNullable(jsonNode, "executions");
            @Nullable String filePattern = getNullable(jsonNode, "filePattern");
            AddPlugin addPlugin = new AddPlugin(groupId, artifactId, version, configuration, dependencies, executions, filePattern);
            return addPlugin;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNullable(JsonNode jsonNode, String field) {
        return jsonNode.get(field) != null ? jsonNode.get(field).textValue() : null;
    }
}

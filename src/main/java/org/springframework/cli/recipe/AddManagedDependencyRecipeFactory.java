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
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddManagedDependency;

/**
 * @author Fabian Krüger
 */
public class AddManagedDependencyRecipeFactory {

    /**
     * Create a {@link AddManagedDependency} recipe from XML snippet of a managed dependency.
     */
    public AddManagedDependency create(String mavenDependencySnippet) {
        try {
            XmlMapper mapper = new XmlMapper();
            JsonNode jsonNode = mapper.readTree(mavenDependencySnippet);
            String groupId = jsonNode.get("groupId").textValue();
            String artifactId = jsonNode.get("artifactId").textValue();
            String version = jsonNode.get("version") != null ? jsonNode.get("version").textValue() : null;
            String scope = jsonNode.get("scope") != null ? jsonNode.get("scope").textValue() : null;
            @Nullable String classifier = jsonNode.get("classifier") != null ? jsonNode.get("classifier").textValue() : null;
            @Nullable Boolean addToRootPom = null;
            AddManagedDependency addManagedDependency = new AddManagedDependency(groupId, artifactId, version, null, scope, classifier, null, true, null, addToRootPom);
            return addManagedDependency;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

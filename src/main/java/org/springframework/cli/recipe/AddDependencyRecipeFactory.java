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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.table.MavenMetadataFailures;

import java.util.regex.Pattern;

/**
 * @author Fabian Krüger
 */
public class AddDependencyRecipeFactory {

    /**
     * Create {@link AddDependency} recipe from Maven dependency XML snippet.
     */
    public Recipe create(String mavenDependency) {
        try {
            XmlMapper mapper = new XmlMapper();
            JsonNode jsonNode = null;
            jsonNode = mapper.readTree(mavenDependency);
            String groupId = jsonNode.get("groupId").textValue();
            String artifactId = jsonNode.get("artifactId").textValue();
            String version = jsonNode.get("version") != null ? jsonNode.get("version").textValue() : "latest";
            String scope = jsonNode.get("scope") != null ? jsonNode.get("scope").textValue() : null;
            String type = jsonNode.get("type") != null ? jsonNode.get("type").textValue() : null;
            @Nullable String classifier = jsonNode.get("classifier") != null ? jsonNode.get("classifier").textValue() : null;
            @Nullable Boolean optional = jsonNode.get("optional") != null ? Boolean.valueOf(jsonNode.get("optional").textValue()) : null;
            @Nullable String familyPattern = null;
            Pattern familyRegex = familyPattern == null ? null : Pattern.compile(familyPattern);
            @Nullable Boolean acceptTransitive = null;
            MavenMetadataFailures metadataFailures = null;
            AddDependencyVisitor addDependencyVisitor = new AddDependencyVisitor(groupId, artifactId, version, null, scope, true, type, classifier, optional, familyRegex, metadataFailures);
            return new Recipe() {

                @Override
                public String getDisplayName() {
                    return "Add dependency '%s:%s'".formatted(groupId, artifactId);
                }

                @Override
                public String getDescription() {
                    return getDisplayName();
                }

                @Override
                public TreeVisitor<?, ExecutionContext> getVisitor() {
                    return addDependencyVisitor;
                }
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}

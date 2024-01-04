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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.table.MavenMetadataFailures;

import java.util.regex.Pattern;

/**
 * Alternative to {@link org.openrewrite.maven.AddDependency} not doing any version checks.
 * It uses the {@link AddDependencyVisitor} and bypasses all checks and other code in {@link org.openrewrite.maven.AddManagedDependency}.
 *
 * @author Fabian Krüger
 */
public class AddDependencyRecipe extends Recipe {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String scope;
    private final String type;
    private final @Nullable String classifier;
    private final @Nullable Boolean optional;
    private final Pattern familyRegex;
    private final MavenMetadataFailures metadataFailures;

    public AddDependencyRecipe(String groupId, String artifactId, String version, String scope, String type, @Nullable String classifier, @Nullable Boolean optional, Pattern familyRegex, MavenMetadataFailures metadataFailures) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
        this.optional = optional;
        this.familyRegex = familyRegex;
        this.metadataFailures = metadataFailures;
    }

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
        AddDependencyVisitor addDependencyVisitor = new AddDependencyVisitor(groupId, artifactId, version, null, scope, true, type, classifier, optional, familyRegex, metadataFailures);
        return addDependencyVisitor;
    }
}

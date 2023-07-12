/*
 * Copyright 2021 the original author or authors.
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


package org.springframework.cli.merger;

import java.util.List;
import java.util.regex.Pattern;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

public class AddSimpleDependencyRecipe extends Recipe {

	private final String groupId;

	private final String artifactId;

	private final String version;

	private final String versionPattern;

	private final String scope;

	private final Boolean releasesOnly;

	private final String onlyIfUsing;

	private final String type;

	private final String classifier;

	private final Boolean optional;

	private final String familyPattern;

	public AddSimpleDependencyRecipe(final String groupId, final String artifactId, final String version, final @Nullable String versionPattern, final @Nullable String scope, final @Nullable Boolean releasesOnly, final String onlyIfUsing, final @Nullable String type, final @Nullable String classifier, final @Nullable Boolean optional, final @Nullable String familyPattern) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.versionPattern = versionPattern;
		this.scope = scope;
		this.releasesOnly = releasesOnly;
		this.onlyIfUsing = onlyIfUsing;
		this.type = type;
		this.classifier = classifier;
		this.optional = optional;
		this.familyPattern = familyPattern;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getVersionPattern() {
		return versionPattern;
	}

	public String getScope() {
		return scope;
	}

	public Boolean getReleasesOnly() {
		return releasesOnly;
	}

	public String getOnlyIfUsing() {
		return onlyIfUsing;
	}

	public String getType() {
		return type;
	}

	public String getClassifier() {
		return classifier;
	}

	public Boolean getOptional() {
		return optional;
	}

	public String getFamilyPattern() {
		return familyPattern;
	}

	@Override
	public String getDisplayName() {
		return "Add Simple Maven Dependency";
	}

	@Override
	public String getDescription() {
		return "Add Simple Maven Dependency";
	}


	//TODO OR UPGRADE
	protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {

		Pattern familyPatternCompiled = this.familyPattern == null ? null : Pattern.compile(this.familyPattern.replace("*", ".*"));

		return ListUtils.map(before, s -> s.getMarkers().findFirst(MavenResolutionResult.class)
				.map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
					@Override
					public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
						Xml maven = super.visitDocument(document, executionContext);

						return new AddSimpleDependencyVisitor(
								getGroupId(), getArtifactId(), getVersion(), getVersionPattern(), getScope(), getReleasesOnly(),
								getType(), getClassifier(), getOptional(), familyPatternCompiled).visitNonNull(s, ctx);
					}
				}.visit(s, ctx))
				.map(SourceFile.class::cast)
				.orElse(s)
		);
	}
}

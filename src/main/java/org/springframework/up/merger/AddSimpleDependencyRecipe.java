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


package org.springframework.up.merger;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.tree.Xml;

import org.openrewrite.maven.AddDependency;

import java.util.List;
import java.util.regex.Pattern;

public class AddSimpleDependencyRecipe extends AddDependency {
	public AddSimpleDependencyRecipe(String groupId, String artifactId, String version, @Nullable String versionPattern, @Nullable String scope, @Nullable Boolean releasesOnly, String onlyIfUsing, @Nullable String type, @Nullable String classifier, @Nullable Boolean optional, @Nullable String familyPattern) {
		super(groupId, artifactId, version, versionPattern, scope, releasesOnly, onlyIfUsing, type, classifier, optional, familyPattern);
	}

	@Override
	protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
		return null;
	}

	@Override
	protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {

		Pattern familyPatternCompiled = this.getFamilyPattern() == null ? null : Pattern.compile(this.getFamilyPattern().replace("*", ".*"));

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

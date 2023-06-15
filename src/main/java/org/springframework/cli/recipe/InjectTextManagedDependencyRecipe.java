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


package org.springframework.cli.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.AddManagedDependencyVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;

/**
 * This subclass is needed since recent changes to the openrewrite recpie have strict
 * version checks and a version such as ${spring-cloud-version} can't be used.
 * This is a cut-n-paste replacement that disables the version checking, making sure only
 * that the field has a not null value.
 */
public class InjectTextManagedDependencyRecipe extends AddManagedDependency {

	private final String text;
	public InjectTextManagedDependencyRecipe(String text) {
		super(null, null, null, null, null,
				null, null, null, null, null);
		this.text = text;
	}

	@Override
	protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
		List<SourceFile> rootPoms = new ArrayList<>();
		for (SourceFile source : before) {
			source.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(mavenResolutionResult -> {
				if (mavenResolutionResult.getParent() == null) {
					rootPoms.add(source);
				}
			});
		}

		return ListUtils.map(before, s -> s.getMarkers().findFirst(MavenResolutionResult.class)
				.map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
					@Override
					public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
						Xml maven = super.visitDocument(document, executionContext);
						if (!Boolean.TRUE.equals(getAddToRootPom()) || rootPoms.contains(document)) {
							doAfterVisit(new InjectTextManagedDependencyVisitor(text));
							maybeUpdateModel();
						}
						return maven;
					}

				}.visit(s, ctx))
				.map(SourceFile.class::cast)
				.orElse(s)
		);
	}
}

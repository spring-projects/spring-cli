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

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

public class InjectTextMavenDependencyRecipe extends Recipe {

	private final String text;

	public InjectTextMavenDependencyRecipe(String text) {
		this.text = text;
	}

	@Override
	public String getDisplayName() {
		return "Inject Text As Maven Dependency";
	}

	@Override
	public String getDescription() {
		return "Inject Text As Maven Dependency";
	}

	//TODO OR UPGRADE
	protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {

		return ListUtils.map(before, s -> s.getMarkers().findFirst(MavenResolutionResult.class)
				.map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
					@Override
					public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
						super.visitDocument(document, executionContext);
						return new InjectTextMavenDependencyVisitor(text).visitNonNull(s, ctx);
					}
				}.visit(s, ctx))
				.map(SourceFile.class::cast)
				.orElse(s)
		);
	}
}

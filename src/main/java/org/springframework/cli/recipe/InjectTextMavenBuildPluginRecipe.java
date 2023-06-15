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

import java.util.Optional;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;

public class InjectTextMavenBuildPluginRecipe extends Recipe {

	private static final XPathMatcher BUILD_MATCHER = new XPathMatcher("/project/build");

	private String text;

	public InjectTextMavenBuildPluginRecipe(String text) {
		this.text = text;
	}

	@Override
	public String getDisplayName() {
		return "Add Build Plugin";
	}

	protected TreeVisitor<?, ExecutionContext> getVisitor() {
		return new AddPluginVisitor();
	}

	private class AddPluginVisitor extends MavenIsoVisitor<ExecutionContext> {

		@Override
		public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
			Xml.Tag root = document.getRoot();
			if (!root.getChild("build").isPresent()) {
				document = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<build/>"))
						.visitNonNull(document, ctx, getCursor().getParentOrThrow());
			}
			return super.visitDocument(document, ctx);
		}

		@Override
		public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
			Xml.Tag t = super.visitTag(tag, ctx);

			if (BUILD_MATCHER.matches(getCursor())) {
				Optional<Xml.Tag> maybePlugins = t.getChild("plugins");
				Xml.Tag plugins;
				if (maybePlugins.isPresent()) {
					plugins = maybePlugins.get();
				}
				else {
					t = (Xml.Tag) new AddToTagVisitor<>(t, Xml.Tag.build("<plugins/>")).visitNonNull(t, ctx, getCursor().getParentOrThrow());
					plugins = t.getChild("plugins").get();
				}
				Xml.Tag pluginTag = Xml.Tag.build(text);
				t = (Xml.Tag) new AddToTagVisitor<>(plugins, pluginTag).visitNonNull(t, ctx, getCursor().getParentOrThrow());
			}

			return t;
		}
	}
}

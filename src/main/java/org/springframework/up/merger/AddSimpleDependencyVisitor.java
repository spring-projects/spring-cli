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

import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenTagInsertionComparator;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.emptyList;

public class AddSimpleDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {

	private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");

	private final String groupId;
	private final String artifactId;
	private final String version;

	@Nullable
	private final String versionPattern;

	@Nullable
	private final String scope;

	@Nullable
	private final Boolean releasesOnly;

	@Nullable
	private final String type;

	@Nullable
	private final String classifier;

	@Nullable
	private final Boolean optional;

	@Nullable
	private final Pattern familyRegex;

	@Nullable
	private VersionComparator versionComparator;

	@Nullable
	private String resolvedVersion;

	public AddSimpleDependencyVisitor(String groupId, String artifactId, String version, @Nullable String versionPattern, @Nullable String scope, @Nullable Boolean releasesOnly, @Nullable String type, @Nullable String classifier, @Nullable Boolean optional, @Nullable Pattern familyRegex) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.versionPattern = versionPattern;
		this.scope = scope;
		this.releasesOnly = releasesOnly;
		this.type = type;
		this.classifier = classifier;
		this.optional = optional;
		this.familyRegex = familyRegex;
	}

	@Override
	public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
		Xml.Document maven = super.visitDocument(document, executionContext);


		Xml.Tag root = maven.getRoot();
		if (!root.getChild("dependencies").isPresent()) {
			doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies/>"),
					new MavenTagInsertionComparator(root.getContent() == null ? emptyList() : root.getContent())));
		}

		doAfterVisit(new InsertDependencyInOrder(scope));

		return maven;
	}

	private class InsertDependencyInOrder extends MavenVisitor<ExecutionContext> {

		@Nullable
		private final String scope;

		public InsertDependencyInOrder(@Nullable String scope) {
			this.scope = scope;
		}

		@Override
		public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
			if (DEPENDENCIES_MATCHER.matches(getCursor())) {

				String versionToUse = version;

				Xml.Tag dependencyTag = Xml.Tag.build(
						"\n<dependency>\n" +
								"<groupId>" + groupId + "</groupId>\n" +
								"<artifactId>" + artifactId + "</artifactId>\n" +
								(versionToUse == null ? "" :
										"<version>" + versionToUse + "</version>\n") +
								(classifier == null ? "" :
										"<classifier>" + classifier + "</classifier>\n") +
								(scope == null || "compile".equals(scope) ? "" :
										"<scope>" + scope + "</scope>\n") +
								(Boolean.TRUE.equals(optional) ? "<optional>true</optional>\n" : "") +
								"</dependency>"
				);

				doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
						new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));
				maybeUpdateModel();

				return tag;
			}

			return super.visitTag(tag, ctx);
		}

	}

}

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

import java.util.Optional;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenTagInsertionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;

import org.springframework.cli.util.ProjectInfo;
import org.springframework.util.StringUtils;

/**
 * Update GAV, descriptions and name if they are specified.
 */
public class ChangeNewlyClonedPomRecipe extends Recipe {

	static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

	private ProjectInfo projectInfo;

	public ChangeNewlyClonedPomRecipe(ProjectInfo projectInfo) {
		this.projectInfo = projectInfo;
	}

	@Override
	public String getDisplayName() {
		return "Change Group, Artifact, Version, Name, and Description";
	}

	@Override
	public String getDescription() {
		return "Update GAV, project name and description.";
	}

	public ProjectInfo getProjectInfo() {
		return projectInfo;
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return new XmlVisitor<ExecutionContext>() {

			@Override
			public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
				if (PROJECT_MATCHER.matches(getCursor())) {
					visit(tag, "groupId", projectInfo.getGroupId());
					visit(tag, "artifactId", projectInfo.getArtifactId());
					visit(tag, "version", projectInfo.getVersion());
					visit(tag, "name", projectInfo.getName());
					visit(tag, "description", projectInfo.getDescription());
				}
				return super.visitTag(tag, ctx);
			}

			private void visit(Tag rootTag, String tagName, String tagValueToReplaceOrAdd) {
				if (!StringUtils.hasText(tagValueToReplaceOrAdd)) {
					return;
				}
				Optional<Tag> tagToReplaceOrAdd = rootTag.getChild(tagName);
				if (tagToReplaceOrAdd.isPresent()) {
					if (!tagValueToReplaceOrAdd.equals(tagToReplaceOrAdd.get().getValue().orElse(null))) {
						doAfterVisit(new ChangeTagValueVisitor<>(tagToReplaceOrAdd.get(), tagValueToReplaceOrAdd));
					}
				}
				else {
					doAfterVisit(new AddToTagVisitor<>(rootTag,
							Tag.build("<" + tagName + ">" + tagValueToReplaceOrAdd + "</" + tagName + ">"),
							new MavenTagInsertionComparator(rootTag.getChildren())));
				}
			}
		};
	}

}

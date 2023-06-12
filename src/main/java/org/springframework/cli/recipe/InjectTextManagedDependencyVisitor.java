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

import java.util.Collections;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenTagInsertionComparator;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;

public class InjectTextManagedDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {

	private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

	private final String text;


	public InjectTextManagedDependencyVisitor(String text) {
		this.text = text;
	}

	@Override
	public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
		Xml.Document doc = super.visitDocument(document, ctx);
		if (false) {
			return document;
		} else {
			Xml.Tag root = document.getRoot();
			List<? extends Content> rootContent = root.getContent() != null ? root.getContent() : Collections.emptyList();
			Xml.Tag dependencyManagementTag = (Xml.Tag)root.getChild("dependencyManagement").orElse(null);
			if (dependencyManagementTag == null) {
				doc = (Xml.Document)(new AddToTagVisitor(root, Tag.build("<dependencyManagement>\n<dependencies/>\n</dependencyManagement>"), new MavenTagInsertionComparator(rootContent))).visitNonNull(doc, ctx);
			} else if (!dependencyManagementTag.getChild("dependencies").isPresent()) {
				doc = (Xml.Document)(new AddToTagVisitor(dependencyManagementTag, Tag.build("\n<dependencies/>\n"), new MavenTagInsertionComparator(rootContent))).visitNonNull(doc, ctx);
			}
			doc = (Xml.Document) (new InjectTextManagedDependencyVisitor.InsertDependencyInOrder(text)).visitNonNull(doc, ctx);
			//doc = (Xml.Document)(new AddManagedDependencyVisitor.InsertDependencyInOrder(this.groupId, this.artifactId, this.version, this.type, this.scope, this.classifier))
			// .visitNonNull(doc, ctx);
			return doc;
		}
	}

	private static class InsertDependencyInOrder extends MavenIsoVisitor<ExecutionContext> {
		private final String text;

		public InsertDependencyInOrder(String text) {
			this.text = text;
		}

		public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
			if (InjectTextManagedDependencyVisitor.MANAGED_DEPENDENCIES_MATCHER.matches(this.getCursor())) {
				Xml.Tag dependencyTag = Tag.build(text);
//						"\n<dependency>\n<groupId>" +
//								this.groupId + "</groupId>\n<artifactId>" +
//								this.artifactId + "</artifactId>\n<version>" +
//								this.version + "</version>\n" +
//								(this.classifier == null ? "" : "<classifier>" + this.classifier +
//										"</classifier>\n") + (this.type != null && !this.type.equals("jar") ? "<type>" +
//								this.type + "</type>\n" : "") + (this.scope != null && !"compile".equals(this.scope) ? "<scope>" +
//								this.scope + "</scope>\n" : "") + "</dependency>");
				this.doAfterVisit(new AddToTagVisitor(tag, dependencyTag, new InsertDependencyComparator(tag.getContent() == null ? Collections.emptyList() : tag.getContent(), dependencyTag)));
			}

			return super.visitTag(tag, ctx);
		}

	}
}

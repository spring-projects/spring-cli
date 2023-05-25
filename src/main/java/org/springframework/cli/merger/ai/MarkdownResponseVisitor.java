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


package org.springframework.cli.merger.ai;

import java.util.ArrayList;
import java.util.List;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Paragraph;

import org.springframework.cli.SpringCliException;

public class MarkdownResponseVisitor extends AbstractVisitor {

	private List<ProjectArtifact> projectArtifacts = new ArrayList<>();

	@Override
	public void visit(FencedCodeBlock fencedCodeBlock) {
		String info = fencedCodeBlock.getInfo();
		String code = fencedCodeBlock.getLiteral();
		if (info.equalsIgnoreCase("java")) {
			addJavaCode(code);
		}
		if (info.equalsIgnoreCase("xml")) {
			addMavenDependencies(code);
		}
		if (info.equalsIgnoreCase("properties")) {
			addApplicationProperties(code);
		}
		if (info.equalsIgnoreCase("html")) {
			addHtml(code);
		}
		if (info.isBlank()) {
			// sometimes the response doesn't contain
			if (code.contains("package")) {
				addJavaCode(code);
			} else if (code.contains("<dependency>")) {
				addMavenDependencies(code);
			}
		}

	}

	private void addHtml(String code) {
		addIfNotPresent(new ProjectArtifact(ProjectArtifactType.HTML, code));
	}

	private void addApplicationProperties(String code) {
		addIfNotPresent(new ProjectArtifact(ProjectArtifactType.APPLICATION_PROPERTIES, code));
	}

	private void addMavenDependencies(String code) {
		addIfNotPresent(new ProjectArtifact(ProjectArtifactType.MAVEN_DEPENDENCIES, code));
	}

	private void addJavaCode(String code) {
		if (code.contains("@SpringBootApplication") && code.contains("SpringApplication.run")) {
			addIfNotPresent(new ProjectArtifact(ProjectArtifactType.MAIN_CLASS, code));
		} else if (code.contains("@Test")) {
			addIfNotPresent(new ProjectArtifact(ProjectArtifactType.TEST_CODE, code));
		} else {
			addIfNotPresent(new ProjectArtifact(ProjectArtifactType.SOURCE_CODE, code));
		}
	}

	private void addIfNotPresent(ProjectArtifact projectArtifact) {
		// TODO - investigate why Node has duplicate entries
		if (!this.projectArtifacts.contains(projectArtifact)) {
			this.projectArtifacts.add(projectArtifact);
		} else {
			//System.out.println("duplicate project artifact :(");
		}
	}

	public List<ProjectArtifact> getProjectArtifacts() {
		return this.projectArtifacts;
	}
}

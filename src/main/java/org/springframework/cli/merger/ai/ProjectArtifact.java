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

import java.util.Objects;

public class ProjectArtifact {

	private ProjectArtifactType artifactType;

	private String text;

	public ProjectArtifact(ProjectArtifactType artifactType, String text) {
		this.artifactType = artifactType;
		this.text = text;
	}

	public ProjectArtifactType getArtifactType() {
		return artifactType;
	}

	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProjectArtifact that = (ProjectArtifact) o;
		return artifactType == that.artifactType && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifactType, text);
	}
}

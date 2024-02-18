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

public class ProjectDependency {

	private final String groupId;

	private final String artifactId;

	public ProjectDependency(String groupId, String artifactId) {
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public String toString() {
		return "ProjectDependency{" + "groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + '}';
	}

}

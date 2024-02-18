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

package org.springframework.cli.util;

import org.springframework.util.StringUtils;

public class ProjectInfo {

	private String name;

	private String description;

	private String groupId;

	private String artifactId;

	private String version;

	private String packageName;

	public ProjectInfo(String groupId, String artifactId, String version, String name, String description,
			String packageName) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.name = name;
		this.description = description;
		this.packageName = packageName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public String getPackageName() {
		return packageName;
	}

	@Override
	public String toString() {
		return "ProjectInfo{" + "name='" + name + '\'' + ", description='" + description + '\'' + ", groupId='"
				+ groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", version='" + version + '\''
				+ ", packageName='" + packageName + '\'' + '}';
	}

	public ProjectInfo getDefaults() {
		// if no artifact name is specified, fall back to using the name, which may or
		// may not be specified.
		String artifactIdToUse = (StringUtils.hasText(artifactId)) ? artifactId : name;

		// if no package name is specified, try to derive from groupId and artifactId
		String packageNameToUse;
		if (StringUtils.hasText(packageName)) {
			packageNameToUse = packageName;
		}
		else if (StringUtils.hasText(groupId) && StringUtils.hasText(artifactIdToUse)) {
			packageNameToUse = groupId + "." + artifactIdToUse;
		}
		else {
			packageNameToUse = null;
		}
		return new ProjectInfo(groupId, artifactIdToUse, version, name, description, packageNameToUse);
	}

}

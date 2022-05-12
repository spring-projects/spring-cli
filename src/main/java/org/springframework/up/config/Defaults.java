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


package org.springframework.up.config;

public class Defaults {

	private String projectName;
	private String packageName;
	private String templateRepositoryName;

	public Defaults() {
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getTemplateRepositoryName() {
		return templateRepositoryName;
	}

	public void setTemplateRepositoryName(String templateRepositoryName) {
		this.templateRepositoryName = templateRepositoryName;
	}

	@Override
	public String toString() {
		return "Defaults{" +
				"projectName='" + projectName + '\'' +
				", packageName='" + packageName + '\'' +
				", templateRepositoryName='" + templateRepositoryName + '\'' +
				'}';
	}
}

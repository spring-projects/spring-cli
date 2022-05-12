/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.up.initializr.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectType {

	@JsonProperty("default")
	private String defaultType;
	private String action;
	private List<ProjectTypeValue> values = new ArrayList<>();

	public ProjectType() {
	}

	public String getDefault() {
		return defaultType;
	}

	public void setDefault(String defaultType) {
		this.defaultType = defaultType;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public List<ProjectTypeValue> getValues() {
		return values;
	}

	public void setValues(List<ProjectTypeValue> values) {
		this.values = values;
	}

	@Override
	public String toString() {
		return "ProjectType [action=" + action + ", defaultType=" + defaultType + ", values=" + values + "]";
	}

	public static class ProjectTypeValue extends IdName {

		private String action;

		private String description;

		private Map<String, String> tags = new HashMap<>();

		public ProjectTypeValue() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Map<String, String> getTags() {
			return tags;
		}

		public void setTags(Map<String, String> tags) {
			this.tags = tags;
		}

		@Override
		public String toString() {
			return "ProjectTypeValue [action=" + action + ", description=" + description + ", tags=" + tags + "]";
		}
	}
}

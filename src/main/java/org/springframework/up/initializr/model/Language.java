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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Language {

	@JsonProperty("default")
	private String defaultType;
	private String type;
	List<LanguageValues> values = new ArrayList<>();

	public Language() {
	}

	public String getDefault() {
		return defaultType;
	}

	public void setDefault(String defaultType) {
		this.defaultType = defaultType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<LanguageValues> getValues() {
		return values;
	}

	public void setValues(List<LanguageValues> values) {
		this.values = values;
	}

	public static class LanguageValues extends IdName {

		public LanguageValues() {
		}
	}
}

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

package org.springframework.cli.runtime.engine.actions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;

/**
 * Subsection of the `Exec` action that specifies a model variable to define as the
 * result of executing the command.
 * The expectation is that the result of executing the command is a JSON object, so
 * a JSON-Path is specified to parse our the variable value.
 */
public class Define {

	/**
	 * The name of the model variable to define
	 */
	@Nullable
	private final String name;

	/**
	 * The Json-path expression to use to specify the value of the model variable
	 */
	@Nullable
	private final String jsonPath;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	Define(@JsonProperty("name") String name, @JsonProperty("jsonPath") String jsonPath) {
		this.name = name;
		this.jsonPath = jsonPath;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getJsonPath() {
		return jsonPath;
	}

	@Override
	public String toString() {
		return "Define{" +
				"name='" + name + '\'' +
				", jsonPath='" + jsonPath + '\'' +
				'}';
	}
}

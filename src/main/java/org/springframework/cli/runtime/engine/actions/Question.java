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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;

public class Question {

	private String name;

	private String label;

	private String type;

	@Nullable
	private Options options;

	@Nullable
	private Attributes attributes;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	Question(@JsonProperty("name") String name, @JsonProperty("label") String label, @JsonProperty("type") String type,
			@JsonProperty("options") Options options, @JsonProperty("attributes") Attributes attributes) {
		this.name = Objects.requireNonNull(name);
		this.label = Objects.requireNonNull(label);
		this.type = Objects.requireNonNullElse(type, "input");
		this.options = options;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	@Nullable
	public String getType() {
		return type;
	}

	@Nullable
	public Options getOptions() {
		return options;
	}

	@Nullable
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "Question{" + "name='" + name + '\'' + ", label='" + label + '\'' + ", type='" + type + '\''
				+ ", options=" + options + ", attributes=" + attributes + '}';
	}

}

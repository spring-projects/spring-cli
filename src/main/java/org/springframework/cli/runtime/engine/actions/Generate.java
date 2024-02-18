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

public class Generate {

	private final String to;

	private final String text;

	private final String from;

	/**
	 * If set to false, generation of the template is skipped if the {@link #generate
	 * destination file} already exists.
	 */
	private boolean overwrite;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	Generate(@JsonProperty("to") String to, @JsonProperty("text") String text, @JsonProperty("from") String from,
			@JsonProperty("overwrite") boolean overwrite) {
		this.to = to;
		this.text = text;
		this.from = from;
		this.overwrite = overwrite;
	}

	public String getTo() {
		return to;
	}

	public String getText() {
		return text;
	}

	public String getFrom() {
		return from;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	@Override
	public String toString() {
		return "Generate{" + "to='" + to + '\'' + ", text='" + text + '\'' + ", from='" + from + '\'' + ", overwrite="
				+ overwrite + '}';
	}

}

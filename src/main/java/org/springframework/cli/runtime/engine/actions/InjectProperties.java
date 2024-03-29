/*
 * Copyright 2019 the original author or authors.
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

/**
 * @author Mark Pollack
 */
public class InjectProperties {

	private final String to;

	private final String skip;

	public String getTo() {
		return to;
	}

	public String getSkip() {
		return skip;
	}

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	InjectProperties(@JsonProperty("to") String to, @JsonProperty("skip") String skip) {
		this.to = Objects.requireNonNull(to);
		this.skip = Objects.requireNonNull(skip);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("InjectProperties{");
		sb.append("to='").append(to).append('\'');
		sb.append(", skip='").append(skip).append('\'');
		sb.append('}');
		return sb.toString();
	}

}

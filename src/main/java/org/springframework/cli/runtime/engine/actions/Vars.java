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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;

public class Vars {

	private List<Question> questions;

	public Map<String, Object> data;

	// files

	Vars(@JsonProperty("questions") @Nullable List<Question> questions,
			@JsonProperty("data") @Nullable Map<String, Object> data) {
		this.questions = questions;
		this.data = data;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public Map<String, Object> getData() {
		return data;
	}

	@Override
	public String toString() {
		return "Vars{" +
				"questions=" + questions +
				", data=" + data +
				'}';
	}
}

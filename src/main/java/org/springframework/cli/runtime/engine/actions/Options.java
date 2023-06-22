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

public class Options {

	@Nullable
	private Exec exec;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	Options(@JsonProperty("exec") @Nullable Exec exec) {
		this.exec = exec;
	}

	@Nullable
	public Exec getExec() {
		return exec;
	}

	@Override
	public String toString() {
		return "Options{" +
				"exec=" + exec +
				'}';
	}
}


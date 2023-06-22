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

import java.io.File;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;

/**
 * An action to run an external process, optionally sending its output to a file and/or
 * using the body of the template as its stdin.
 */
public class Exec {

	/**
	 * If set, the relative path to which to redirect stdout of the running process.
	 */
	@Nullable
	private final String to;

	/**
	 * If set, the relative path to which to redirect stderr of the running process.
	 */
	@Nullable
	private final String errto;

	/**
	 * Use the rendered body of the field as stdin of the running process.
	 */
	private String stdIn;

	/**
	 * The command to run/execute.
	 */
	@Nullable
	private String command;

	@Nullable
	private String commandFile;

	/**
	 * If set, the path (relative to the current engine working directory) to use as the
	 * {@link ProcessBuilder#directory(File) working directory} of the running process.
	 */
	@Nullable
	private final String dir;

	private final String jsonPath;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public Exec(@JsonProperty("to") String to,
			@JsonProperty("command") String command,
			@JsonProperty("command-file") String commandFile,
			//TODO change to "error-to"
			@JsonProperty("errto") String errto,
			@JsonProperty("dir") String dir,
			@JsonProperty("json-path") String jsonPath) {
		this.to = to;
		this.command = command;
		this.commandFile = commandFile;
		this.errto = errto;
		this.dir = Objects.requireNonNullElse(dir, "");
		this.jsonPath = jsonPath;
	}

	@Nullable
	public String getTo() {
		return to;
	}

	public String getCommand() {
		return command;
	}

	@Nullable
	public String getCommandFile() {
		return commandFile;
	}

	@Nullable
	public String getErrto() {
		return errto;
	}

	public String getDir() {
		return dir;
	}

	@Nullable
	public String getJsonPath() {
		return jsonPath;
	}

	@Override
	public String toString() {
		return "Exec{" +
				"to='" + to + '\'' +
				", errto='" + errto + '\'' +
				", stdIn='" + stdIn + '\'' +
				", command='" + command + '\'' +
				", commandFile='" + commandFile + '\'' +
				", dir='" + dir + '\'' +
				", jsonPath='" + jsonPath + '\'' +
				'}';
	}
}

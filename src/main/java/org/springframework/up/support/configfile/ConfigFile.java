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
package org.springframework.up.support.configfile;

import java.nio.file.Path;

/**
 * Contract to read and write a config file. This contract doesn't care how
 * things are stored as it's a decision for implementation but generally
 * speaking interface expects one file per type which makes it easier to
 * store complex types if/when needed.
 *
 * @author Janne Valkealahti
 */
public interface ConfigFile {

	/**
	 * Read typed mapped class from a config.
	 *
	 * @param <T> the type of a class to map
	 * @param path the path to config file
	 * @param type type of a class
	 * @return deserialized content
	 */
	<T> T read(Path path, Class<T> type);

	/**
	 * Write new content into a config file
	 *
	 * @param path the path to config file
	 * @param value the value to write
	 */
	void write(Path path, Object value);
}

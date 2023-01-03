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
package org.springframework.cli.support.configfile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.cli.SpringCliException;

public class YamlConfigFile implements ConfigFile {

	private final ObjectMapper mapper;

	public YamlConfigFile() {
		mapper = new ObjectMapper(new YAMLFactory());
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	@Override
	public <T> T read(Path path, Class<T> type) {
		try {
			InputStream in = new DataInputStream(Files.newInputStream(path));
			return mapper.readValue(in, type);
		} catch (Exception e) {
			throw new RuntimeException("Unable to read YAML file from path " + path, e);
		}
	}

	@Override
	public void write(Path path, Object value) {
		try {
			OutputStream out = new DataOutputStream(Files.newOutputStream(path));
			mapper.writeValue(out, value);
		} catch (Exception e) {
			throw new RuntimeException("Unable to write YAML file to path " + path, e);
		}
	}
}

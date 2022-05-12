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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

public class YamlConfigFileTests {

	@Test
	public void testReadWrite(@TempDir Path tempDir) {
		Path file = tempDir.resolve("file.yml");
		YamlConfigFile yamlConfigFile = new YamlConfigFile();
		yamlConfigFile.write(file, Bean1.of("value1", "key2", "value2"));
		Bean1 bean = yamlConfigFile.read(file, Bean1.class);
		assertThat(bean.getValue()).isEqualTo("value1");
		assertThat(bean.getValues()).containsEntry("key2", "value2");
	}

	private static class Bean1 {

		private String value;
		private Map<String, String> values;

		static Bean1 of(String value, String valuesKey, String valuesValue) {
			Bean1 bean = new Bean1();
			bean.setValue(value);
			Map<String, String> values = new HashMap<>();
			values.put(valuesKey, valuesValue);
			bean.setValues(values);
			return bean;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public Map<String, String> getValues() {
			return values;
		}

		public void setValues(Map<String, String> values) {
			this.values = values;
		}
	}
}

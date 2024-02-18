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

package org.springframework.cli.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassNameExtractorTests {

	@Test
	void extractClassName() {
		String fileContent = "public class MyClass {\n" + "    // Class implementation\n" + "}\n";

		ClassNameExtractor classNameExtractor = new ClassNameExtractor();
		String className = classNameExtractor.extractClassName(fileContent).get();

		assertThat(className).isEqualTo("MyClass");

		fileContent = "package com.example.restservice.ai.jpa;\n" + "\n"
				+ "import org.springframework.data.repository.CrudRepository;\n" + "\n"
				+ "public interface PersonRepository extends CrudRepository<Person, Long> {\n" + "\n" + "}";

		String interfaceName = classNameExtractor.extractClassName(fileContent).get();
		assertThat(interfaceName).isEqualTo("PersonRepository");
	}

}

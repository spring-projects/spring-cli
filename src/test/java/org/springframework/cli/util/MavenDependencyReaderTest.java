package org.springframework.cli.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

class MavenDependencyReaderTest {

	private String dependencyText = "<dependency>\n" + "  <groupId>org.springframework.boot</groupId>\n"
			+ "  <artifactId>spring-boot-starter-data-jpa</artifactId>\n" + "</dependency>\n" + "\n" + "<dependency>\n"
			+ "  <groupId>org.springframework.boot</groupId>\n"
			+ "  <artifactId>spring-boot-starter-test</artifactId>\n" + "  <scope>test</scope>\n" + "</dependency>\n"
			+ "\n" + "<dependency>\n" + "  <groupId>com.h2database</groupId>\n" + "  <artifactId>h2</artifactId>\n"
			+ "  <scope>runtime</scope>\n" + "</dependency>\n";

	@Test
	void read() {
		MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
		String[] mavenDependencies = mavenDependencyReader.parseMavenSection(dependencyText);
		assertThat(mavenDependencies).hasSize(3);
		assertThat(mavenDependencies[0]).isEqualTo("<dependency>\n" + "  <groupId>org.springframework.boot</groupId>\n"
				+ "  <artifactId>spring-boot-starter-data-jpa</artifactId>\n" + "</dependency>");
		assertThat(mavenDependencies[1]).isEqualTo("<dependency>\n" + "  <groupId>org.springframework.boot</groupId>\n"
				+ "  <artifactId>spring-boot-starter-test</artifactId>\n" + "  <scope>test</scope>\n"
				+ "</dependency>");
		assertThat(mavenDependencies[2]).isEqualTo("<dependency>\n" + "  <groupId>com.h2database</groupId>\n"
				+ "  <artifactId>h2</artifactId>\n" + "  <scope>runtime</scope>\n" + "</dependency>");
	}

}
/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.cli.recipe;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.maven.AddPlugin;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
class AddPluginRecipeFactoryTests {

	private AddPluginRecipeFactory sut = new AddPluginRecipeFactory();

	@Test
	@DisplayName("should create Recipe from valid XML")
	void shouldCreateRecipeFromValidXml() {
		@Language("xml")
		String snippet = """
				<plugin>
					<groupId>groupId</groupId>
					<artifactId>artifactId</artifactId>
					<version>version</version>
					<configuration>configuration</configuration>
					<dependencies>dependencies</dependencies>
					<executions>executions</executions>
					<filePattern>filePattern</filePattern>
				</plugin>
				""";
		AddPlugin recipe = sut.create(snippet);
		assertThat(ReflectionTestUtils.getField(recipe, "groupId").toString()).isEqualTo("groupId");
		assertThat(ReflectionTestUtils.getField(recipe, "artifactId").toString()).isEqualTo("artifactId");
		assertThat(ReflectionTestUtils.getField(recipe, "version").toString()).isEqualTo("version");
		assertThat(ReflectionTestUtils.getField(recipe, "configuration").toString()).isEqualTo("configuration");
		assertThat(ReflectionTestUtils.getField(recipe, "dependencies").toString()).isEqualTo("dependencies");
		assertThat(ReflectionTestUtils.getField(recipe, "executions").toString()).isEqualTo("executions");
		assertThat(ReflectionTestUtils.getField(recipe, "filePattern").toString()).isEqualTo("filePattern");
	}

}

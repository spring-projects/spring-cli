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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.testutil.TestResourceUtils;
import org.springframework.core.io.ClassPathResource;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionsFileReaderTests {

//	@Test
//	void readWithName() {
//		ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(getClass(), "actions-with-name.yaml");
//		ActionFileReader actionFileReader = new ActionFileReader();
//		ActionsFile actionsFile = actionFileReader.read(classPathResource);
//		System.out.println(actionsFile.getActions());
//	}

	@Test
	void readBadVars() {
		ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(getClass(), "bad-vars.yaml");
		ActionFileReader actionFileReader = new ActionFileReader();

		Exception exception = assertThrows(SpringCliException.class, () -> {
			ActionsFile actionsFile = actionFileReader.read(classPathResource);
		});

		assertThat(exception.getMessage()).contains("You may have forgot to define 'name' or 'label' as fields directly under the '-question:' field");
	}

	@Test
	void readKeyValueVars() {
		ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(getClass(), "vars-data.yaml");
		ActionFileReader actionFileReader = new ActionFileReader();
		ActionsFile actionsFile = actionFileReader.read(classPathResource);
		assertThat(actionsFile.getActions()).hasSize(1);
		Action action = actionsFile.getActions().get(0);
		Vars vars = action.getVars();
		Map<String, Object> data = vars.getData();
		assertThat(data)
				.containsEntry("name", "John")
				.containsEntry("age", 30)
				.containsEntry("person", true);

	}

	@Test
	void readVars() {
		ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(getClass(), "vars.yaml");
		ActionFileReader actionFileReader = new ActionFileReader();
		ActionsFile actionsFile = actionFileReader.read(classPathResource);
		assertThat(actionsFile.getActions()).hasSize(1);
		Action action = actionsFile.getActions().get(0);
		Vars vars = action.getVars();
		Question question = vars.getQuestions().get(0);
		assertThat(vars.getQuestions()).hasSize(1);
		assertThat(question.getName()).isEqualTo("resource-group");
		assertThat(question.getLabel()).isEqualTo("Select a resource group.");
		assertThat(question.getType()).isEqualTo("dropdown");
		Options options = question.getOptions();
		assertThat(options.getExec()).isNotNull();
		assertThat(options.getExec().getCommand()).isEqualTo("az group list");
		assertThat(options.getExec().getJsonPath()).isEqualTo("$[*].name");
	}

	@Test
	void readInjectMavenDep() {
		ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(getClass(), "inject-maven-deps.yaml");

		ActionFileReader actionFileReader = new ActionFileReader();
		ActionsFile actionsFile = actionFileReader.read(classPathResource);
		assertThat(actionsFile.getActions()).hasSize(1);
		String text = actionsFile.getActions().get(0).getInjectMavenDependency().getText();
		assertThat(text).contains("spring-boot-starter-data-jpa")
				.contains("com.h2database")
				.contains("spring-boot-starter-test");
	}


//	public static String asString(Resource resource) {
//		try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
//			return FileCopyUtils.copyToString(reader);
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
}

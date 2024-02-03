package org.springframework.cli.merger.ai;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cli.merger.ai.service.GenerateCodeAiService;
import org.springframework.cli.testutil.TestResourceUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
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

public class OpenAiHandlerTests {


	@Test
	void add() throws IOException {
		OpenAiHandler openAiHandler = new OpenAiHandler(new GenerateCodeAiService(TerminalMessage.noop()));
		ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(getClass(), "response.md");
		String response = StreamUtils.copyToString(classPathResource.getInputStream(), UTF_8);
		assertThat(response).isNotNull();

		List<ProjectArtifact> projectArtifacts = openAiHandler.computeProjectArtifacts(response);
//		ResponseModifier responseModifier = new ResponseModifier();
//		String modifiedResponse = responseModifier.modify(response, "fake");
//		assertThat(modifiedResponse).isNotNull();
		assertThat(projectArtifacts).hasSize(5);
	}
}
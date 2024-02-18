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

package org.springframework.cli.merger.ai.service;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.merger.ai.ProjectName;
import org.springframework.cli.merger.ai.PromptRequest;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

public class ProjectNameHeuristicAiService extends AbstractOpenAiService {

	public ProjectNameHeuristicAiService(TerminalMessage terminalMessage) {
		super(terminalMessage);
	}

	public ProjectName deriveProjectName(String description) {
		Map<String, String> context = getContext(description);
		String response = generate(context);
		return createProjectName(response);
	}

	@Override
	public String generate(Map<String, String> context) {
		PromptRequest promptRequest = createPromptRequest(context, "name-heuristic");
		ChatCompletionRequest chatCompletionRequest = getChatCompletionRequest(promptRequest);
		return getResponse(chatCompletionRequest);
	}

	private ProjectName createProjectName(String response) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = null;
		try {
			map = mapper.readValue(response, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (JsonProcessingException ex) {
			throw new SpringCliException("Can read JSON response: " + response, ex);
		}
		String springProjectName = (String) map.get("springProjectName");
		String shortName = (String) map.get("shortName");
		if (!StringUtils.hasText(springProjectName)) {
			springProjectName = "Spring Project";
		}
		if (!StringUtils.hasText(shortName)) {
			shortName = "ai";
		}
		return new ProjectName(shortName.toLowerCase(), springProjectName);

	}

}

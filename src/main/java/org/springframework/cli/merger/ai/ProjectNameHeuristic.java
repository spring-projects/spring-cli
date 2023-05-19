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


package org.springframework.cli.merger.ai;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.templating.HandlebarsTemplateEngine;
import org.springframework.cli.util.PropertyFileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ProjectNameHeuristic {

	private final HandlebarsTemplateEngine handlebarsTemplateEngine = new HandlebarsTemplateEngine();

	public ProjectName deriveProjectName(String description) {
		// get api token in file ~/.openai
		Properties properties = PropertyFileUtils.getPropertyFile();
		String apiKey = properties.getProperty("OPEN_AI_API_KEY");
		OpenAiService openAiService = new OpenAiService(apiKey, Duration.of(5, ChronoUnit.MINUTES));

		Map<String, String> context = getContext(description);

		PromptRequest promptRequest = createPromptRequest(context);
		ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
				.builder()
				.model("gpt-3.5-turbo")
				.temperature(0.8)
				.messages(
						List.of(
								new ChatMessage("system", promptRequest.getSystemPrompt()),
								new ChatMessage("user", promptRequest.getUserPrompt())))
				.build();

		StringBuilder builder = new StringBuilder();
		openAiService.createChatCompletion(chatCompletionRequest)
				.getChoices().forEach(choice -> {
					builder.append(choice.getMessage().getContent());
				});

		String response = builder.toString();
		return createProjectName(response);
	}

	private ProjectName createProjectName(String response) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = null;
		try {
			map = mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
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

	private Map<String, String> getContext(String description) {
		Map<String, String> context = new HashMap<>();
		context.put("description", description);
		return context;
	}

	private PromptRequest createPromptRequest(Map<String, String> context) {
		String systemPrompt = getPrompt(context, "system-name-heuristic");
		String userPrompt = getPrompt(context, "user-name-heuristic");
		return new PromptRequest(systemPrompt, userPrompt);
	}
	private String getPrompt(Map<String, String> context, String promptType) {
		String resourceFileName = "/org/springframework/cli/merger/ai/openai-" + promptType + "-prompt.txt";
		try {
			ClassPathResource promptResource = new ClassPathResource(resourceFileName);
			String promptRaw = StreamUtils.copyToString(promptResource.getInputStream(), UTF_8);
			return this.handlebarsTemplateEngine.process(promptRaw, context);
		}
		catch (FileNotFoundException ex) {
			throw new SpringCliException("Resource file note found:" + resourceFileName);
		}
		catch (IOException ex) {
			throw new SpringCliException("Could read file " + resourceFileName, ex);
		}

	}

}

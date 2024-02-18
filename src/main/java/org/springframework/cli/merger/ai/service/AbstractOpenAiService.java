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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.jetbrains.annotations.NotNull;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.merger.ai.PromptRequest;
import org.springframework.cli.runtime.engine.templating.HandlebarsTemplateEngine;
import org.springframework.cli.util.PropertyFileUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractOpenAiService implements org.springframework.cli.merger.ai.service.OpenAiService {

	private final HandlebarsTemplateEngine handlebarsTemplateEngine = new HandlebarsTemplateEngine();

	private com.theokanning.openai.service.OpenAiService openAiService;

	private final TerminalMessage terminalMessage;

	public AbstractOpenAiService(TerminalMessage terminalMessage) {
		this.terminalMessage = terminalMessage;
	}

	protected ChatCompletionRequest getChatCompletionRequest(PromptRequest promptRequest) {
		createOpenAiService();
		ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
			.model("gpt-3.5-turbo")
			.temperature(0.3)
			.messages(List.of(new ChatMessage("system", promptRequest.getSystemPrompt()),
					new ChatMessage("user", promptRequest.getUserPrompt())))
			.build();
		return chatCompletionRequest;
	}

	private void createOpenAiService() {
		if (this.openAiService == null) {
			// get api token in file ~/.openai
			Properties properties = PropertyFileUtils.getPropertyFile();
			String apiKey = properties.getProperty("OPEN_AI_API_KEY");
			this.openAiService = new OpenAiService(apiKey, Duration.of(5, ChronoUnit.MINUTES));
		}
	}

	public OpenAiService getOpenAiService() {
		return openAiService;
	}

	public HandlebarsTemplateEngine getHandlebarsTemplateEngine() {
		return handlebarsTemplateEngine;
	}

	public TerminalMessage getTerminalMessage() {
		return terminalMessage;
	}

	protected Map<String, String> getContext(String description) {
		Map<String, String> context = new HashMap<>();
		context.put("description", description);
		return context;
	}

	protected String getPrompt(Map<String, String> context, String promptType) {
		String resourceFileName = "/org/springframework/cli/merger/ai/openai-" + promptType + "-prompt.txt";
		try {
			ClassPathResource promptResource = new ClassPathResource(resourceFileName);
			String promptRaw = StreamUtils.copyToString(promptResource.getInputStream(), UTF_8);
			return getHandlebarsTemplateEngine().process(promptRaw, context);
		}
		catch (FileNotFoundException ex) {
			throw new SpringCliException("Resource file note found:" + resourceFileName);
		}
		catch (IOException ex) {
			throw new SpringCliException("Could read file " + resourceFileName, ex);
		}

	}

	protected PromptRequest createPromptRequest(Map<String, String> context, String promptFamilyName) {
		String systemPrompt = getPrompt(context, "system-" + promptFamilyName);
		String userPrompt = getPrompt(context, "user-" + promptFamilyName);
		return new PromptRequest(systemPrompt, userPrompt);
	}

	@NotNull
	protected String getResponse(ChatCompletionRequest chatCompletionRequest) {
		StringBuilder builder = new StringBuilder();
		getOpenAiService().createChatCompletion(chatCompletionRequest).getChoices().forEach(choice -> {
			builder.append(choice.getMessage().getContent());
		});

		String response = builder.toString();
		return response;
	}

}

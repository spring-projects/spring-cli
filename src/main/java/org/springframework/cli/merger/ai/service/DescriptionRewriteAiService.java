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

import com.theokanning.openai.completion.chat.ChatCompletionRequest;

import org.springframework.cli.merger.ai.PromptRequest;
import org.springframework.cli.util.TerminalMessage;

public class DescriptionRewriteAiService extends AbstractOpenAiService {

	public DescriptionRewriteAiService(TerminalMessage terminalMessage) {
		super(terminalMessage);
	}

	public String rewrite(String description) {
		Map<String, String> context = getContext(description);
		return generate(context);
	}

	@Override
	public String generate(Map<String, String> context) {
		PromptRequest promptRequest = createPromptRequest(context, "rewrite-description");
		ChatCompletionRequest chatCompletionRequest = getChatCompletionRequest(promptRequest);
		return getResponse(chatCompletionRequest);
	}

}

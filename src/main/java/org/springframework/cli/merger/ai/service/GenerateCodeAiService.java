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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;

import org.springframework.cli.merger.ai.PromptRequest;
import org.springframework.cli.merger.ai.ResponseModifier;
import org.springframework.cli.util.TerminalMessage;

public class GenerateCodeAiService extends AbstractOpenAiService {

	public GenerateCodeAiService(TerminalMessage terminalMessage) {
		super(terminalMessage);
	}

	public String generate(Map<String, String> context) {
		PromptRequest promptRequest = createPromptRequest(context, "ai-add");
		String completionEstimate = getCompletionEstimate();
		getTerminalMessage()
			.print("Generating code.  This will take a few minutes ..." + " Check back around " + completionEstimate);
		ChatCompletionRequest chatCompletionRequest = getChatCompletionRequest(promptRequest);
		String response = getResponse(chatCompletionRequest);
		ResponseModifier responseModifier = new ResponseModifier();
		return responseModifier.modify(response, context.get("description"));
	}

	private String getCompletionEstimate() {
		TimeZone userTimeZone = TimeZone.getDefault();
		LocalTime estimatedTime = LocalTime.now().plusMinutes(3);
		DateTimeFormatter formatter;
		if (userTimeZone.inDaylightTime(new java.util.Date())) {
			// If the user's time zone is in daylight saving time, use 12-hour format with
			// AM/PM
			formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
		}
		else {
			// Otherwise, use 24-hour format
			formatter = DateTimeFormatter.ofPattern("HH:mm");
		}
		return estimatedTime.format(formatter);
	}

}

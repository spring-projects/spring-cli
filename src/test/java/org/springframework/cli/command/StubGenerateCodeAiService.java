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

package org.springframework.cli.command;

import java.io.IOException;
import java.util.Map;

import org.springframework.cli.merger.ai.service.GenerateCodeAiService;
import org.springframework.cli.testutil.TestResourceUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StubGenerateCodeAiService extends GenerateCodeAiService {

	public StubGenerateCodeAiService(TerminalMessage terminalMessage) {
		super(terminalMessage);
	}

	@Override
	public String generate(Map<String, String> context) {
		try {
			ClassPathResource classPathResource = TestResourceUtils.qualifiedResource(StubGenerateCodeAiService.class,
					"response.md");
			String response = StreamUtils.copyToString(classPathResource.getInputStream(), UTF_8);
			return response;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}

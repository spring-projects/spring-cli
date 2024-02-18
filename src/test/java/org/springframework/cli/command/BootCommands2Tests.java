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

package org.springframework.cli.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.rewrite.RewriteRecipeLauncher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author Fabian Krüger
 */
@ExtendWith(MockitoExtension.class)
class BootCommands2Tests {

	private BootCommands sut;

	@Mock
	private SourceRepositoryService repoService;

	@Mock
	private RewriteRecipeLauncher recipeRunner;

	@Mock
	private TerminalMessage terminalMessage;

	@Mock
	private SpringCliUserConfig userConfig;

	@BeforeEach
	void beforeEach() {
		sut = new BootCommands(userConfig, repoService, terminalMessage, recipeRunner);
	}

	@Test
	void bootUpgrade() {
		String path = "";
		sut.bootUpgrade(path);
		verify(recipeRunner).run(eq("org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_1"), eq(path),
				ArgumentMatchers.isA(RewriteRecipeLauncher.RewriteRecipeRunnerProgressListener.class));
	}

}

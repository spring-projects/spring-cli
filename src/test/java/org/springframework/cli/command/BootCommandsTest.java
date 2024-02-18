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
class BootCommandsTest {

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
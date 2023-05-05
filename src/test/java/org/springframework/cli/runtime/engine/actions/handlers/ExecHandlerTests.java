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


package org.springframework.cli.runtime.engine.actions.handlers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.CommandRunner;
import org.springframework.cli.support.MockConfigurations.MockBaseConfig;
import org.springframework.cli.support.MockConfigurations.MockUserConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecHandlerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockBaseConfig.class);

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void simpleExec(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			// Precondition that directory doesn't exist
			String tmpDir = System.getProperty("java.io.tmpdir");
			Path dir = Paths.get(tmpDir, "foobar");
			FileUtils.deleteDirectory(dir.toFile());
			assertThat(dir).doesNotExist();

			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("exec")
					.executeCommand("util/mkdir")
					.withArguments("directory-to-create", dir.toFile().getAbsolutePath())
					.build();
			commandRunner.run();

			// Assert that the exec action created the directory
			assertThat(dir).exists().isDirectory();
		});
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void testDefineVar(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir,
			@TempDir(cleanup = CleanupMode.ALWAYS) Path tempPath) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			File tempFile = new File(tempPath.toFile(), "temp.txt");

			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("exec-define")
					.executeCommand("define/var")
					.withArguments("output-temp-file", tempFile.getAbsolutePath())
					.build();
			commandRunner.run();

			assertThat(tempFile).hasContent("Hello iPhone");
		});
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void redirection(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir,
			@TempDir(cleanup = CleanupMode.ALWAYS) Path tempPath) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			Path outputPath = tempPath.resolve("result");
			Path commandPath = Path.of("test-data")
					.resolve("commands").resolve("exec-redirect")
					.resolve("working").resolve("dir");

			CommandRunner commandRunner = new CommandRunner.Builder(context)
					.prepareProject("rest-service", workingDir)
					.installCommandGroup("exec-redirect")
					.executeCommand("working/dir")
					.withArguments("output", outputPath.toAbsolutePath().toString())
					.withArguments("work-dir", commandPath.toString())
					.build();
			commandRunner.run();

			assertThat(outputPath).hasContent("ls-action.yml");
		});
	}
}

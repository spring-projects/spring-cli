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

import java.nio.file.Files;
import java.nio.file.Path;

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

public class InjectHandlerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockBaseConfig.class);

	@Test
	void injectBefore(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			Path sampleFile = Path.of("test-data")
				.resolve("commands")
				.resolve("inject")
				.resolve("before")
				.resolve("inject")
				.resolve("sample.txt");
			Path destinationPath = workingDir.resolve("sample.txt");

			Files.copy(sampleFile, destinationPath);
			CommandRunner commandRunner = new CommandRunner.Builder(context).prepareProject("rest-service", workingDir)
				.installCommandGroup("inject")
				.executeCommand("before/inject")
				.build();
			commandRunner.run();

			assertThat(destinationPath).exists();
			String expectedContents = "hello there\n" + "this is a test file\n" + "INJECTED BEFORE\n"
					+ "we are going to insert before the line that has the word marker1\n" + "marker2";
			assertThat(destinationPath.toFile()).hasContent(expectedContents);

		});
	}

	@Test
	void injectAfter(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			Path sampleFile = Path.of("test-data")
				.resolve("commands")
				.resolve("inject")
				.resolve("after")
				.resolve("inject")
				.resolve("sample.txt");
			Path destinationPath = workingDir.resolve("sample.txt");

			Files.copy(sampleFile, destinationPath);
			CommandRunner commandRunner = new CommandRunner.Builder(context).prepareProject("rest-service", workingDir)
				.installCommandGroup("inject")
				.executeCommand("after/inject")
				.build();
			commandRunner.run();

			assertThat(destinationPath).exists();
			String expectedContents = "hello there\n" + "this is a test file\n"
					+ "we are going to insert before the line that has the word marker1\n" + "marker2\n"
					+ "INJECTED AFTER";
			assertThat(destinationPath.toFile()).hasContent(expectedContents);

		});
	}

	@Test
	void injectSkip(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path workingDir) {
		this.contextRunner.withUserConfiguration(MockUserConfig.class).run((context) -> {

			Path sampleFile = Path.of("test-data")
				.resolve("commands")
				.resolve("inject")
				.resolve("skip")
				.resolve("inject")
				.resolve("sample.txt");
			Path destinationPath = workingDir.resolve("sample.txt");

			Files.copy(sampleFile, destinationPath);
			CommandRunner commandRunner = new CommandRunner.Builder(context).prepareProject("rest-service", workingDir)
				.installCommandGroup("inject")
				.executeCommand("skip/inject")
				.build();
			commandRunner.run();

			assertThat(destinationPath).exists();
			String expectedContents = "hello there\n" + "this is a test file\n"
					+ "we are going to insert before the line that has the word marker1\n" + "marker2";
			assertThat(destinationPath.toFile()).hasContent(expectedContents);

		});
	}

}

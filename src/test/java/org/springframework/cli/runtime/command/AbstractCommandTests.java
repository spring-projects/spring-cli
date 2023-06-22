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


package org.springframework.cli.runtime.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.util.TerminalMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractCommandTests {

	public void runCommand(String noun, String verb, Map<String, Object> model, String commandLocation) throws IOException {
		DynamicCommand dynamicCommand = new DynamicCommand(noun, verb, Collections.emptyList(), TerminalMessage.noop(), Optional.empty());
		dynamicCommand.runCommand(Paths.get(commandLocation), ".spring", "commands", model);
	}

	public void runCommand(String noun, String verb, Map<String, Object> model, List<ModelPopulator> modelPopulators, String commandLocation) throws IOException {
		DynamicCommand dynamicCommand = new DynamicCommand(noun, verb, modelPopulators, TerminalMessage.noop(), Optional.empty());
		dynamicCommand.runCommand(Paths.get(commandLocation), ".spring", "commands", model);
	}

	public void runInjectAction(String noun, String verb, Map<String, Object> model, String fileName, String commandLocation) throws IOException {
		Path workingDirectory = new File(commandLocation).toPath();
		assertThat(Files.exists(workingDirectory)).isTrue();

		// Construct Paths
		String baseFileName = workingDirectory.toString() + "/" + fileName;
		Path fileToInject = Paths.get(baseFileName + ".txt");
		assertThat(Files.exists(fileToInject)).isTrue();
		Path expectedFile = Paths.get(baseFileName + ".txt.expected");
		assertThat(Files.exists(expectedFile)).isTrue();
		Path failedFile = Paths.get(baseFileName + ".txt.failed");
		if (Files.exists(failedFile)) {
			// Delete from previous run
			Files.delete(failedFile);
		}
		Path backupFile = Paths.get(baseFileName + ".txt.bak");

		// Create backup file
		Files.copy(fileToInject, backupFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

		try {
			DynamicCommand dynamicCommand = new DynamicCommand(noun, verb, Collections.emptyList(), TerminalMessage.noop(), Optional.empty());
			dynamicCommand.runCommand(Paths.get(commandLocation), null, null, model);
			List<String> expectedLines = Files.readAllLines(expectedFile);
			List<String> actualLines = Files.readAllLines(fileToInject);
			Patch<String> patch = DiffUtils.diff(actualLines, expectedLines);

			boolean failed = patch.getDeltas().size() > 0;
			if (failed) {
				// failed keep modified file with .failed suffix, restore original file
				// from backup and delete backup
				Files.copy(fileToInject, failedFile, StandardCopyOption.COPY_ATTRIBUTES,
						StandardCopyOption.REPLACE_EXISTING);
				Files.copy(backupFile, fileToInject, StandardCopyOption.COPY_ATTRIBUTES,
						StandardCopyOption.REPLACE_EXISTING);
				Files.delete(backupFile);

				List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(baseFileName + ".txt.expected",
						baseFileName + ".txt.failed", actualLines, patch, 0);
				unifiedDiff.forEach(System.out::println);
				assertThat(unifiedDiff).as("File diff is not empty").containsExactlyElementsOf(new ArrayList<String>());
				// show that test failed to assertj framework
				// assertThat(failed).as("Files not modified/created as
				// expected.").isFalse();

			}
			else {
				// clean up - copy backup file to original file name and delete backup
				// file.
				Files.copy(backupFile, fileToInject, StandardCopyOption.COPY_ATTRIBUTES,
						StandardCopyOption.REPLACE_EXISTING);
				Files.delete(backupFile);
				assertThat(failed).as("Files modified/created as expected.").isFalse();

			}

		}
		catch (Exception ex) {
			System.out.println(ex);
			ex.printStackTrace();
			assertThat(ex).as("Error running test.").isNotNull();
		}

	}




	public void deleteIfExists(Path dir) throws IOException {
		if (Files.exists(dir)) {
			// Delete from previous run
			Files.delete(dir);
		}
	}
}

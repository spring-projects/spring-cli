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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.cli.util.IoUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicCommandExecTests {

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void mkdirTests() throws IOException {
		String tmpDir = System.getProperty("java.io.tmpdir");
		Path dir = Paths.get(tmpDir, "foobar");
		try {
			deleteIfExists(dir);
			Map<String, Object> model = new HashMap<>();
			model.put("directory-to-create", dir.toFile().getAbsolutePath());
			runCommand("util", "mkdir", model, "src/test/resources/org/springframework/cli/runtime/command/exec");
			assertThat(dir).exists().isDirectory();
		}
		finally {
			deleteIfExists(dir);
		}
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void testRedirections(@TempDir Path tmp) throws IOException {
		Path result = tmp.resolve("result");
		Map<String, Object> model = new HashMap<>();
		model.put("output", result.toAbsolutePath().toString());
		// Run 'ls' in the dir of the ls.txt file itself.
		model.put("work-dir", "src/test/resources/org/springframework/cli/runtime/command/exec/working/dir");
		runCommand("working", "dir", model, "src/test/resources/org/springframework/cli/runtime/command/exec");
		assertThat(result).hasContent("ls.txt");
	}

	public void runCommand(String noun, String verb, Map<String, Object> model, String commandLocation) throws IOException {
		DynamicCommand dynamicCommand = new DynamicCommand(noun, verb, Collections.emptyList());
		dynamicCommand.runCommand(Paths.get(commandLocation), null, null, null, model);
	}


	private void deleteIfExists(Path dir) throws IOException {
		if (Files.exists(dir)) {
			// Delete from previous run
			Files.delete(dir);
		}
	}

}

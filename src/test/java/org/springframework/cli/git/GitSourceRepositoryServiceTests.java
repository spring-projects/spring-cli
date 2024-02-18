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

package org.springframework.cli.git;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.cli.config.SpringCliUserConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class GitSourceRepositoryServiceTests {

	/**
	 * Simple sanity test
	 * @param tempDir location to put contents of git repository
	 */
	@Test
	void testRetrieval(@TempDir Path tempDir) throws IOException {
		GitSourceRepositoryService urlRepositoryService = new GitSourceRepositoryService(new SpringCliUserConfig());
		Path contentPath = urlRepositoryService.retrieveRepositoryContents("https://github.com/rd-1-2022/rest-service");
		assertThat(PathUtils.isEmpty(contentPath)).isFalse();
		String[] pathToFile = new String[] { "src", "main", "java", "com", "example", "restservice", "greeting",
				"GreetingController.java" };
		Path greetingControllerPath = Paths.get(contentPath.toString(), pathToFile);
		assertThat(PathUtils.isEmpty(greetingControllerPath)).isFalse();
	}

}

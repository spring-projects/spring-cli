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

package org.springframework.up.git;

import java.nio.file.Path;

/**
 * @author Thomas Risberg
 */
public interface SourceRepositoryService {

	/**
	 * Retrieve contents from a source repository.
	 * @param sourceRepoUrl the URL of the repository to retrieve the content for.
	 * Supported URL schemes are file:// and https://. The latter can refer to a GitHub
	 * (host is github.com) repository or a GitLab repository.
	 * @return the full Path to where the contents have been retrieved
	 */
	Path retrieveRepositoryContents(String sourceRepoUrl);

}

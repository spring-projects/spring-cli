/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Thomas Risberg
 */
public class GitRepoUrlRefTests {

	@Test
	void testIllegalArgs() {
		assertThatIllegalArgumentException().isThrownBy(() -> GitRepoUrlRef.fromUriString(null));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> GitRepoUrlRef.fromUriString("github.com/trisberg/hello-fun"));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> GitRepoUrlRef.fromUriString("git://github.com/trisberg/hello-fun"));
		assertThatIllegalArgumentException().isThrownBy(() -> GitRepoUrlRef.fromUriString("https://"));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> GitRepoUrlRef.fromUriString("git://github.com/trisberg/hello-fun?fail=true"));
	}

	@Test
	void testGitRepoUri() {
		String gitRepoUrl = "https://github.com/trisberg/hello-fun";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://github.com/trisberg/hello-fun");
		assertThat(url.getRef()).isNull();
		assertThat(url.getSubPath()).isNull();
	}

	@Test
	void testGitRepoUriWithQuery() {
		String gitRepoUrl = "https://github.com/trisberg/hello-fun?ref=v0.1.0&subPath=my-path";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://github.com/trisberg/hello-fun");
		assertThat(url.getRef()).isEqualTo("v0.1.0");
		assertThat(url.getSubPath()).isEqualTo("my-path");
	}

	@Test
	void testGitHubWebUrl() {
		String gitRepoUrl = "https://github.com/simple-starters/hello-fun/tree/v0.1.0";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://github.com/simple-starters/hello-fun.git");
		assertThat(url.getRef()).isEqualTo("v0.1.0");
		assertThat(url.getSubPath()).isNull();
	}

	@Test
	void testGitHubWebUrlWithSubPath() {
		String gitRepoUrl = "https://github.com/trisberg/fun-fun/tree/main/hello-fun";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://github.com/trisberg/fun-fun.git");
		assertThat(url.getRef()).isEqualTo("main");
		assertThat(url.getSubPath()).isEqualTo("hello-fun");
	}

	@Test
	void testGitLabWebUrlBranch() {
		String gitRepoUrl = "https://gitlab.eng.vmware.com/risbergt/my-hello-fun/-/tree/test";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://gitlab.eng.vmware.com/risbergt/my-hello-fun.git");
		assertThat(url.getRef()).isEqualTo("test");
		assertThat(url.getSubPath()).isNull();
	}

	@Test
	void testGitLabWebUrlTag() {
		String gitRepoUrl = "https://gitlab.eng.vmware.com/risbergt/my-hello-fun/-/tags/v0.1.0";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://gitlab.eng.vmware.com/risbergt/my-hello-fun.git");
		assertThat(url.getRef()).isEqualTo("v0.1.0");
		assertThat(url.getSubPath()).isNull();
	}

	@Test
	void testGitLabWebUrlWithSubPath() {
		String gitRepoUrl = "https://gitlab.eng.vmware.com/risbergt/fun-fun/-/tree/v0.1.0/hello-fun";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
		assertThat(url.getRepoUrl().toString()).isEqualTo("https://gitlab.eng.vmware.com/risbergt/fun-fun.git");
		assertThat(url.getRef()).isEqualTo("v0.1.0");
		assertThat(url.getSubPath()).isEqualTo("hello-fun");
	}

	@Test
	void testBadWebUrl() {
		String gitRepoUrl = "https://github.com/simple-starters/hello-fun/bad/test";
		GitRepoUrlRef url = GitRepoUrlRef.fromUriString(gitRepoUrl);
		assertThat(url.getRepoUrl()).isNotNull();
	}

}

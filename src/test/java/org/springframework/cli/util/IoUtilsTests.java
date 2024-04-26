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

package org.springframework.cli.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IoUtilsTests {

	private FileSystem fileSystem;

	@BeforeEach
	public void setupTests() {
		fileSystem = Jimfs.newFileSystem();
	}

	@Test
	public void inProjectRootDirectoryJustPom() throws IOException {
		Path root = fileSystem.getPath("");
		Path pom = root.resolve("pom.xml");
		Files.createFile(pom);
		boolean inProjectRootDirectory = IoUtils.inProjectRootDirectory(root);
		assertThat(inProjectRootDirectory).isTrue();
	}

	@Test
	public void testInProjectRootDirectoryDotgitfile() throws IOException {
		Path root = fileSystem.getPath("");
		Path dotgit = root.resolve(".git");
		Files.createFile(dotgit);
		boolean inProjectRootDirectory = IoUtils.inProjectRootDirectory(root);
		assertThat(inProjectRootDirectory).isTrue();
	}

	@Test
	public void testInProjectRootDirectoryDotgitdir() throws IOException {
		Path root = fileSystem.getPath("");
		Path dotgit = root.resolve(".git");
		Files.createDirectories(dotgit);
		boolean inProjectRootDirectory = IoUtils.inProjectRootDirectory(root);
		assertThat(inProjectRootDirectory).isTrue();
	}

	@Test
	public void testInProjectRootDirectoryPomAndDotgitfile() throws IOException {
		Path root = fileSystem.getPath("");
		Path pom = root.resolve("pom.xml");
		Path dotgit = root.resolve(".git");
		Files.createFile(pom);
		Files.createFile(dotgit);
		boolean inProjectRootDirectory = IoUtils.inProjectRootDirectory(root);
		assertThat(inProjectRootDirectory).isTrue();
	}

}

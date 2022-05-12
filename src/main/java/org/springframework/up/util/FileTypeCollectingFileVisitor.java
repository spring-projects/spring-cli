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

package org.springframework.up.util;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * A simple {@link FileVisitor} that collects file that end with a given extension.
 *
 * @author Eric Bottard
 */
public class FileTypeCollectingFileVisitor extends SimpleFileVisitor<Path> {

	private final List<Path> matches = new ArrayList<>();

	private final String extension;

	public FileTypeCollectingFileVisitor(final String extension) {
		this.extension = extension.startsWith(".") ? extension : "." + extension;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		if (attrs.isRegularFile() && file.getFileName().toString().endsWith(extension)) {
			matches.add(file);
		}
		return CONTINUE;
	}

	public List<Path> getMatches() {
		return matches;
	}

}

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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.openrewrite.Result;

import org.springframework.up.UpException;

/**
 * A container for Rewrite {@link Result}s that can perform the actual
 * move/delete/rewrites.
 *
 * @author Eric Bottard
 */
public class ResultsExecutor {

	private final List<Result> results = new ArrayList<>();

	/**
	 * Whether to delete empty dirs recursively upwards after a file has been deleted (or
	 * moved). Other hierarchies can be recreated afterwards as part of file creation.
	 */
	private final boolean deleteEmptyDirs;

	public ResultsExecutor() {
		this(true);
	}

	public ResultsExecutor(boolean deleteEmptyDirs) {
		this.deleteEmptyDirs = deleteEmptyDirs;
	}

	/**
	 * Checks that no surprising results are going to happen, where a file is bot
	 * created/moved AND its new location is also deleted. Or that two or more results
	 * point at the same location.
	 */
	private void check() {
		Set<Path> destinations = new HashSet<>();
		Set<Path> deletions = new HashSet<>();
		for (Result result : results) {
			if (result.getAfter() != null) {
				if (!destinations.add(result.getAfter().getSourcePath())) {
					throw new UpException("Several files end up creating " + result.getAfter().getSourcePath());
				}
			}
			else {
				if (!deletions.add(result.getBefore().getSourcePath())) {
					// This should be even more unexpected than the above
					throw new UpException("Several files end up deleting " + result.getBefore().getSourcePath());
				}
			}
		}
		destinations.retainAll(deletions);
		if (!destinations.isEmpty()) {
			throw new UpException(
					"Files being created/modified would end up being deleted by other recipe at " + destinations);
		}
	}

	public void addAll(Collection<Result> more) {
		results.addAll(more);
	}

	public void execute() throws IOException {
		check();

		for (Result result : results) {
			if (result.getAfter() == null || fileMoved(result)) {
				Files.delete(result.getBefore().getSourcePath());
				if (deleteEmptyDirs) {
					deleteUpwardsUntilNotEmpty(result.getBefore().getSourcePath().getParent());
				}
			}
			if (result.getAfter() != null) {
				Path afterPath = result.getAfter().getSourcePath();
				afterPath.toFile().getParentFile().mkdirs();
				try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(afterPath)) {
					sourceFileWriter.write(result.getAfter().printAll());
				}
			}
		}
	}

	/**
	 * Walks the directory hierarchy upwards and deletes any empty directories.
	 * @param dir the initial directory to consider
	 */
	private void deleteUpwardsUntilNotEmpty(Path dir) throws IOException {
		if (isEmptyDirectory(dir)) {
			Files.delete(dir);
			deleteUpwardsUntilNotEmpty(dir.getParent());
		}
	}

	/**
	 * Returns true if the given path is a directory AND is empty.
	 */
	private boolean isEmptyDirectory(Path path) throws IOException {
		if (!Files.isDirectory(path)) {
			return false;
		}

		try (Stream<Path> entries = Files.list(path)) {
			return entries.findFirst().isEmpty();
		}
	}

	/**
	 * Returns true if the {@code Result} represents a file move.
	 */
	private boolean fileMoved(Result result) {
		return result.getBefore() != null
				&& !result.getBefore().getSourcePath().equals(result.getAfter().getSourcePath());
	}

}

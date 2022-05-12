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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tools.ant.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.up.UpException;

public class RootPackageFinder {

	private static final Logger logger = LoggerFactory.getLogger(RootPackageFinder.class);

	public static Optional<String> findRootPackage(File baseDirectory) {
		String[] fileNames = getFileNames(baseDirectory);

		for (String fileName : fileNames) {
			logger.debug("Looking for @SpringBootApplication in file = " + fileName);
			try {
				File fileToTest = new File(baseDirectory, fileName);
				List<String> lines = Files.lines(fileToTest.toPath())
						.filter(line -> line.contains("@SpringBootApplication"))
						.collect(Collectors.toList());
				if (lines.isEmpty()) {
					continue;
				} else {
					return Optional.of(extractRootPackageName(new File(fileName)));
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new UpException("Exception reading " + fileName + " " + e.getMessage(), e);

			}
		}
		return Optional.empty();
	}


	public static Optional<File> findSpringBootApplicationFile(File baseDirectory) {
		String[] fileNames = getFileNames(baseDirectory);
		for (String fileName : fileNames) {
			logger.debug("Looking for @SpringBootApplication in file = " + fileName);
			try {
				File fileToTest = new File(baseDirectory, fileName);
				List<String> lines = Files.lines(fileToTest.toPath())
						.filter(line -> line.contains("@SpringBootApplication"))
						.collect(Collectors.toList());
				if (lines.isEmpty()) {
					continue;
				} else {
					return Optional.of(new File(baseDirectory, fileName));
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new UpException("Exception reading " + fileName + " " + e.getMessage(), e);

			}
		}
		return Optional.empty();
	}


	private static String[] getFileNames(File baseDirectory) {
		DirectoryScanner ds = new DirectoryScanner();
		String[] includes = { "**\\*.java" };
		ds.setBasedir(baseDirectory);
		ds.setIncludes(includes);
		ds.scan();
		String[] fileNames = ds.getIncludedFiles();
		return fileNames;
	}

	public static String extractRootPackageName(File file) {
		Path rootPackagePath = file.toPath();
		// remove src/main/java
		Path subPath = rootPackagePath.subpath(3, rootPackagePath.getNameCount());
		// remove last file
		Path packagePath = subPath.subpath(0, subPath.getNameCount() - 1);

		// Create package string from Path
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < packagePath.getNameCount(); i++) {
			sb.append(packagePath.getName(i));
			if (i < packagePath.getNameCount() - 1 ) {
				sb.append(".");
			}
		}
		String packageName = sb.toString();
		return packageName;
	}
}

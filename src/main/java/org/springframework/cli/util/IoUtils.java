/*
 * Copyright 2019 the original author or authors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 */
public abstract class IoUtils {

	private final static String TEST_WORKING_DIRECTORY = "TEST_WORKING_DIRECTORY";

	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

	public static void createDirectory(Path directory) {
		if (!Files.exists(directory)) {
			boolean createdDir = directory.toFile().mkdirs();
			if (createdDir) {
				logger.debug("Created directory " + directory.toAbsolutePath());
			}
		}
	}

	public static Path getWorkingDirectory() {

		if (StringUtils.hasText(System.getenv(TEST_WORKING_DIRECTORY))) {
			Path path = Paths.get(TEST_WORKING_DIRECTORY);
			if (Files.isDirectory(path)) {
				return path;
			} else {
				throw new SpringCliException("Environment variable TEST_WORKING_DIRECTORY = " + path.toAbsolutePath().toString() + " is not a directory");
			}
		}
		return Path.of("");

//		try {
//			String homeDir = new File(".").getCanonicalPath();
//			File f = new File(homeDir);
//			Assert.isTrue(f.isDirectory(), "Path " + f.getAbsolutePath() + " must be a directory.");
//			logger.debug("Working directory = " + f.getAbsolutePath());
//			return f;
//		}
//		catch (IOException e) {
//			throw new SpringCliException("Could not determine working directory", e);
//
//		}
	}

	public static void writeToDir(File dir, String fileName, Resource resource) {
		try {
			String data = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
			File file = new File(dir, fileName);
			writeText(file, data);
		}
		catch (IOException e) {
			throw new SpringCliException(
					"Could not write " + resource.getDescription() + " to directory " + dir.toString(), e);
		}

	}

	public static void writeText(File target, String body) {
		try (OutputStream stream = new FileOutputStream(target)) {
			StreamUtils.copy(body, Charset.forName("UTF-8"), stream);
		}
		catch (Exception e) {
			throw new SpringCliException("Cannot write file " + target, e);
		}
	}

}

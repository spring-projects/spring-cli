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

package org.springframework.up.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.up.UpException;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * @author Mark Pollack
 */
public abstract class IoUtils {

	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

	public static void createDirectory(File directory) {
		if (!directory.exists()) {
			boolean createdDir = directory.mkdirs();
			try {
				if (createdDir) {
					logger.debug("Created directory " + directory.getCanonicalPath());
				}
			}
			catch (IOException e) {
				throw new UpException(e.getMessage(), e);
			}
		}
	}

	public static File getWorkingDirectory() {
		try {
			String homeDir = new File(".").getCanonicalPath();
			File f = new File(homeDir);
			Assert.isTrue(f.isDirectory(), "Path " + f.getAbsolutePath() + " must be a directory.");
			logger.debug("Working directory = " + f.getAbsolutePath());
			return f;
		}
		catch (IOException e) {
			throw new UpException("Could not determine working directory", e);

		}
	}

	public static void writeToDir(File dir, String fileName, Resource resource) {
		try {
			String data = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
			File file = new File(dir, fileName);
			writeText(file, data);
		}
		catch (IOException e) {
			throw new UpException(
					"Could not write " + resource.getDescription() + " to directory " + dir.toString(), e);
		}

	}

	public static void writeText(File target, String body) {
		try (OutputStream stream = new FileOutputStream(target)) {
			StreamUtils.copy(body, Charset.forName("UTF-8"), stream);
		}
		catch (Exception e) {
			throw new UpException("Cannot write file " + target, e);
		}
	}

}

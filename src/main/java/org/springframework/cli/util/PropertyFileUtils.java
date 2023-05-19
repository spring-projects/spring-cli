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


package org.springframework.cli.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.compress.utils.IOUtils;

import org.springframework.cli.SpringCliException;

public class PropertyFileUtils {
	public static Properties getPropertyFile() {
		File homeDir = new File(System.getProperty("user.home"));
		File propertyFile = new File(homeDir, ".openai");
		Properties props = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(propertyFile);
			props.load(in);
			return props;
		} catch (IOException ex) {
			throw new SpringCliException("Could not load property file ~/.openai", ex);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}

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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Ryan Baxter
 */
public class PomReader {

	/**
	 * Returns a parsed POM.
	 */
	public Model readPom(File file) {
		File pom = file;
		if (file.isDirectory()) {
			pom = new File(file, "pom.xml");
		}
		if (!pom.exists()) {
			return null;
		}
		String fileText = "";
		try (Reader reader = new FileReader(pom)) {
			if (file.isFile()) {
				fileText = new String(Files.readAllBytes(file.toPath()));
			}
			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			return xpp3Reader.read(reader);
		}
		catch (XmlPullParserException | IOException e) {
			if (file.isFile() && fileText.length() == 0) {
				throw new IllegalStateException("File [" + pom.getAbsolutePath() + "] is empty", e);
			}
			throw new IllegalStateException("Failed to read file: " + pom.getAbsolutePath(), e);
		}
	}

}

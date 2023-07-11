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


package org.springframework.cli.runtime.engine.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.model.Model;

import org.springframework.cli.util.JavaUtils;
import org.springframework.cli.util.PomReader;

public class MavenModelPopulator implements ModelPopulator {

	public static final String ARTIFACT_ID = "artifact-id";

	public static final String ARTIFACT_VERSION = "artifact-version";

	public static final String ARTIFACT_PATH = "artifact-path";

	public static final String MAVEN_MODEL = "maven-model";

	public static final String MAVEN_PROPERTIES = "maven-properties";

	public static final String PROJECT_NAME = "project-name";

	public static final String PROJECT_DESCRIPTION = "project-description";

	public static final String JAVA_VERSION = "java-version";

	@Override
	public void contributeToModel(Path rootDirectory, Map<String, Object> model) {
		Path pomFile = rootDirectory.resolve("pom.xml");
		if (Files.exists(pomFile)) {
			PomReader pomReader = new PomReader();
			Model mavenModel = pomReader.readPom(pomFile.toFile());
			model.putIfAbsent(MAVEN_MODEL, mavenModel);
			model.putIfAbsent(ARTIFACT_ID, mavenModel.getArtifactId());
			model.putIfAbsent(ARTIFACT_VERSION, mavenModel.getVersion());
			String artifactPath = getArtifactPath(pomFile, mavenModel);
			model.putIfAbsent(ARTIFACT_PATH, artifactPath);

			Properties mavenProperties = new Properties();
			// This will take care of properties such as 'java-version'
			for (Entry<Object, Object> kv : mavenModel.getProperties().entrySet()) {
				// can't use 'dots' in template language replacement expressions, change to underscore
				mavenProperties.put(kv.getKey().toString().replace('.', '-'), kv.getValue());
			}
			model.putIfAbsent(MAVEN_PROPERTIES, mavenProperties);
			model.putIfAbsent(PROJECT_NAME, mavenModel.getName());
			model.putIfAbsent(PROJECT_DESCRIPTION, mavenModel.getDescription());
			if (mavenProperties.containsKey("java-version")) {
				model.put(JAVA_VERSION, JavaUtils.getJavaVersion(mavenProperties.getProperty("java-version")));
			}
		}
	}

	private String getArtifactPath(Path pomFile, Model mavenModel) {
		Path artifactPath = Paths.get(pomFile.getParent().toString(), "target", mavenModel.getArtifactId() + "-" + mavenModel.getVersion() + "." + mavenModel.getPackaging());
		return artifactPath.toAbsolutePath().toString();

	}

}

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


package org.springframework.cli.merger;

import java.nio.file.Path;

import org.apache.maven.model.Model;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.util.JavaVersionUtils;

/**
 * When using the spring boot add command, this class is invoked to check if the
 * project to be merged is compatible with the current project.
 *
 * Reasons why the pre-check would fail are incompatible versions of Java or
 * the to be merged project is using gradle and the current project is using maven.
 *
 * Note, currently only checks Java version compatibility for Maven projects
 */
public class MergerPreCheck {

	/**
	 * Checks if the two projects can be merged, if not an exception is thrown.
	 *
	 * @param currentModel the Maven Model of the current project
	 * @param modelToMerge the Maven Model of the project to merge
	 * @param toMergeProjectPath the location of the code base for the project to merge
	 * @throws SpringCliException if the two projects are incompatible.
	 */
	public static void canMergeProject(Model currentModel, Model modelToMerge, Path toMergeProjectPath) {
		checkJavaVersionCompatibility(currentModel, modelToMerge, toMergeProjectPath);
	}

	private static void checkJavaVersionCompatibility(Model currentModel, Model modelToMerge, Path toMergeProjectPath) {
		if (currentModel == null) {
			throw new SpringCliException("Working directory does not contain pom.xml");
		}
		if (!currentModel.getProperties().containsKey("java.version")) {
			throw new SpringCliException("Can not determine the Java project version of the current project." +
					"  Check that maven property 'java.version' is present in pom.xml");
		}
		if (!modelToMerge.getProperties().containsKey("java.version")) {
			throw new SpringCliException("Can not determine the Java project version of the project to add to the current project." +
					"  Check that maven property 'java.version' is present in pom.xml in the Path = " + toMergeProjectPath.toAbsolutePath());
		}
		int javaVersion = JavaVersionUtils.getJavaVersion(currentModel.getProperties().getProperty("java.version"));
		int javaVersionToMerge = JavaVersionUtils.getJavaVersion(modelToMerge.getProperties().getProperty("java.version"));
		if (javaVersionToMerge > javaVersion) {
			throw new SpringCliException("Current project is a Java " + javaVersion +
					" project.  The project to be added is a Java " + javaVersionToMerge +
					" project.  Stopping merge as the code bases are potentially incompatible.");
		}
	}

}

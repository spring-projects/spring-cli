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

package org.springframework.cli.runtime.engine.actions;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.util.FileExtensionUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

public class ActionFileReader {

	public Optional<ActionsFile> read(Path pathToFile) {
		boolean isYamlFile = false;
		String fileExtension = FileExtensionUtils.getExtension(pathToFile.toString());
		if (!fileExtension.isEmpty()) {
			if (fileExtension.equalsIgnoreCase("yaml") || fileExtension.equalsIgnoreCase(("yml"))) {
				isYamlFile = true;
			}
		}
		if (!isYamlFile) {
			return Optional.empty();
		}
		return Optional.of(read(new FileSystemResource(pathToFile)));
	}

	public ActionsFile read(Resource resource) {
		try {
			String actionFileString = asString(resource);
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
				.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readValue(actionFileString, ActionsFile.class);
		}
		catch (JsonProcessingException ex) {
			improveErrorMessage(resource, ex);
			throw new SpringCliException("Could not deserialize action file " + resource.getDescription(), ex);
		}
		catch (IOException ex) {
			throw new SpringCliException("Could not read resource " + resource.getDescription() + " as a String.", ex);
		}
	}

	private void improveErrorMessage(Resource resource, JsonProcessingException jsonProcessingException) {
		try {

			List<String> contents = Files.readAllLines(resource.getFile().toPath());
			checkForMissingColonOnAction(contents, resource, jsonProcessingException);
			checkForMissingArrayAfterActions(contents, resource, jsonProcessingException);
			checkForMissingQuestionTextAndName(contents, resource, jsonProcessingException);
			checkForBadValuesInVarsData(contents, resource, jsonProcessingException);
		}
		catch (IOException ex) {
			throw new SpringCliException("Could not read resource " + resource.getDescription() + " as a String.", ex);
		}
	}

	private void checkForBadValuesInVarsData(List<String> contents, Resource resource,
			JsonProcessingException jsonProcessingException) {
		if (jsonProcessingException.getMessage()
			.contains("org.springframework.cli.runtime.engine.actions.Vars[\"data\"")) {
			JsonLocation location = jsonProcessingException.getLocation();
			int lineNumber = location.getLineNr() - 1;
			int columnNumber = location.getColumnNr();
			String content = contents.get(lineNumber);
			StringBuffer sb = new StringBuffer();
			sb.append("Could not deserialize action file " + resource.getDescription());
			sb.append(System.lineSeparator());
			sb.append("The error occurred at line,column [" + lineNumber + "," + columnNumber + "]  content = ["
					+ content + "]");
			sb.append(System.lineSeparator());
			if (content.contains("{{")) {
				sb.append(
						"You may have forgotten to add double quotes around the value field when using a handlebars template expression");
			}
			throw new SpringCliException(sb.toString());
		}
	}

	private static void checkForMissingQuestionTextAndName(List<String> contents, Resource resource,
			JsonProcessingException jsonProcessingException) {
		if (jsonProcessingException.getMessage()
			.contains(
					"Cannot construct instance of `org.springframework.cli.runtime.engine.actions.Question`, problem: `java.lang.NullPointerException`")) {
			throw new SpringCliException("Could not deserialize action file " + resource.getDescription()
					+ ".  You may have forgot to define 'name' or 'label' as fields directly under the '-question:' field.  "
					+ "Nested exception message is " + jsonProcessingException.getMessage() + ". Location = "
					+ jsonProcessingException.getLocation());
		}
	}

	private static void checkForMissingArrayAfterActions(List<String> contents, Resource resource,
			JsonProcessingException ex) {
		for (String line : contents) {
			if (line.trim().contains("generate:") && !line.trim().contains("-")) {
				throw new SpringCliException("Could not deserialize action file " + resource.getDescription()
						+ ".  You may have forgot to add a '-' in front of 'generate' since an YAML array is needed.  "
						+ "Nested exception message is " + ex.getMessage());
			}
			if (line.trim().contains("exec:") && !line.trim().contains("-")) {
				throw new SpringCliException("Could not deserialize action file " + resource.getDescription()
						+ ".  You may have forgot to add a '-' in front of 'exec' since an YAML array is needed.  "
						+ "Nested exception message is " + ex.getMessage());
			}
		}
	}

	private static void checkForMissingColonOnAction(List<String> contents, Resource resource,
			JsonProcessingException ex) {
		for (String line : contents) {
			if (line.trim().contains("action") && !line.trim().contains("actions:")) {
				throw new SpringCliException("Could not deserialize action file " + resource.getDescription()
						+ ".  You may have forgot to put a colon after the field 'action'.  "
						+ "Nested exception message is " + ex.getMessage());
			}
		}
	}

	private static String asString(Resource resource) throws IOException {
		Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
		return FileCopyUtils.copyToString(reader);
	}

}

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

package org.springframework.cli.merger.ai;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class ResponseModifier {

	public String modify(String response, String description) {
		return modifyMsyqlDependency(modifyJavax(response, description));
	}

	private String modifyJavax(String response, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("*Note: The code provided is just an example and may not be suitable for production use.*");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
		sb.append("Generated on " + LocalDateTime.now().format(formatter));
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("Generated using the description: " + description);
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append(response);
		String code = sb.toString();

		// TODO - determine if Java 17
		if (!code.contains("import javax")) {
			return code;
		}
		return replaceImportStatements(code);
	}

	private static String replaceImportStatements(String content) {
		String[] lines = content.split(System.lineSeparator());
		StringBuilder updatedContent = new StringBuilder();

		for (String line : lines) {
			if (line.trim().startsWith("import ")) {
				String packageName = extractPackageName(line);
				if (!packageName.contains("javax.sql.")) {
					packageName = packageName.replace("javax", "jakarta");
				}
				line = "import " + packageName + ";";
			}
			updatedContent.append(line).append(System.lineSeparator());
		}
		return updatedContent.toString();
	}

	private static String extractPackageName(String importStatement) {
		return importStatement.replace("import", "").replace(";", "").trim();
	}

	private String modifyMsyqlDependency(String response) {
		if (!response.contains("<artifactId>mysql-connector-java</artifactId>")) {
			return response;
		}
		else {
			String s1 = response.replace("<groupId>mysql</groupId>", "<groupId>com.mysql</groupId>");
			String s2 = s1.replace("<artifactId>mysql-connector-java</artifactId>",
					"<artifactId>mysql-connector-j</artifactId>");
			return s2;
		}

	}

}

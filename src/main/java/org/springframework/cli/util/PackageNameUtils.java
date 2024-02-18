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

import java.util.Arrays;

import javax.lang.model.SourceVersion;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PackageNameUtils {

	/**
	 * Get the package name to use for the new project
	 * @param userProvidedPackageName The package name as passed into the cli command
	 * @param defaultPackageName An optional fallback package name
	 * @return The package name to use for the new project
	 */
	public static String getTargetPackageName(String userProvidedPackageName, String defaultPackageName) {

		// Get target package name, fall back to default
		if (!StringUtils.hasText(userProvidedPackageName)) {
			userProvidedPackageName = defaultPackageName;
		}
		String candidate = cleanPackageName(userProvidedPackageName);
		if (!StringUtils.hasText(candidate)) {
			return defaultPackageName;
		}
		if (hasInvalidChar(candidate.replace(".", ""))) {
			return defaultPackageName;
		}
		if (hasReservedKeyword(candidate)) {
			return defaultPackageName;
		}
		else {
			return candidate;
		}

	}

	private static boolean hasInvalidChar(String text) {
		if (!Character.isJavaIdentifierStart(text.charAt(0))) {
			return true;
		}
		if (text.length() > 1) {
			for (int i = 1; i < text.length(); i++) {
				if (!Character.isJavaIdentifierPart(text.charAt(i))) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasReservedKeyword(final String packageName) {
		return Arrays.stream(packageName.split("\\.")).anyMatch(SourceVersion::isKeyword);
	}

	private static String cleanPackageName(String packageName) {
		String[] elements = packageName.trim().replaceAll("-", "").split("\\W+");
		StringBuilder sb = new StringBuilder();
		for (String element : elements) {
			element = element.replaceFirst("^[0-9]+(?!$)", "");
			if (!element.matches("[0-9]+") && sb.length() > 0) {
				sb.append(".");
			}
			sb.append(element);
		}
		return sb.toString();
	}

}

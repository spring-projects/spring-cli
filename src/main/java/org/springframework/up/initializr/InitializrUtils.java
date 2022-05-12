/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.up.initializr;

import java.util.Collections;

import io.spring.initializr.generator.version.Version;
import io.spring.initializr.generator.version.VersionParser;
import io.spring.initializr.generator.version.VersionRange;

import org.springframework.up.initializr.model.Dependency;
import org.springframework.util.StringUtils;

public abstract class InitializrUtils {

	private final static VersionParser VERSION_PARSER_INSTANCE = new VersionParser(Collections.emptyList());

	public static boolean isDependencyCompatible(Dependency dependency, String version) {
		if (!StringUtils.hasText(version) || !StringUtils.hasText(dependency.getVersionRange())) {
			return true;
		}
		Version parsedVersion = VERSION_PARSER_INSTANCE.parse(version);
		VersionRange parsedRange = VERSION_PARSER_INSTANCE.parseRange(dependency.getVersionRange());
		return parsedRange.match(parsedVersion);
	}
}

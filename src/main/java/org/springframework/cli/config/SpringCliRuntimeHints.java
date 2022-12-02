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


package org.springframework.cli.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public class SpringCliRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		registerForMostReflection(hints.reflection(),
				"org.kohsuke.github.GHCommit",
				"org.kohsuke.github.GitHubInteractiveObject",
				"org.kohsuke.github.GHLicense",
				"org.kohsuke.github.GHMyself",
				"org.kohsuke.github.GHObject",
				"org.kohsuke.github.GHPerson",
				"org.kohsuke.github.GHRepository",
				"org.kohsuke.github.GHRepository$GHRepoPermission",
				"org.kohsuke.github.GHUser",
				"org.kohsuke.github.GHVerification",
				"org.kohsuke.github.GitUser"
		);
	}

	private void registerForMostReflection(ReflectionHints reflectionHints, String... classNames) {
		reflectionHints.registerTypes(typeReferences(classNames),
				hint -> {
					hint.withMembers(
							MemberCategory.DECLARED_CLASSES, MemberCategory.DECLARED_FIELDS,
							MemberCategory.PUBLIC_CLASSES, MemberCategory.PUBLIC_FIELDS,
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INTROSPECT_DECLARED_METHODS);
				});
	}

	private List<TypeReference> typeReferences(String... classNames) {
		return Stream.of(classNames).map(TypeReference::of).collect(Collectors.toList());
	}

}

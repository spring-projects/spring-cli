/*
 * Copyright 2021-2023 the original author or authors.
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

import org.openrewrite.internal.EncodingDetectingInputStream;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.cli.initializr.model.ArtifactId;
import org.springframework.cli.initializr.model.BootVersion;
import org.springframework.cli.initializr.model.Dependencies;
import org.springframework.cli.initializr.model.Dependency;
import org.springframework.cli.initializr.model.DependencyCategory;
import org.springframework.cli.initializr.model.Description;
import org.springframework.cli.initializr.model.GroupId;
import org.springframework.cli.initializr.model.IdName;
import org.springframework.cli.initializr.model.JavaVersion;
import org.springframework.cli.initializr.model.JavaVersion.JavaVersionValues;
import org.springframework.cli.initializr.model.Language;
import org.springframework.cli.initializr.model.Language.LanguageValues;
import org.springframework.cli.initializr.model.Metadata;
import org.springframework.cli.initializr.model.Name;
import org.springframework.cli.initializr.model.PackageName;
import org.springframework.cli.initializr.model.Packaging;
import org.springframework.cli.initializr.model.Packaging.PackagingValues;
import org.springframework.cli.initializr.model.ProjectType;
import org.springframework.cli.initializr.model.ProjectType.ProjectTypeValue;
import org.springframework.cli.initializr.model.Version;

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
		registerForMostReflection(hints.reflection(), ArtifactId.class, BootVersion.class, Dependencies.class,
				Dependency.class, DependencyCategory.class, Description.class, GroupId.class, IdName.class,
				JavaVersion.class, JavaVersionValues.class, Language.class, LanguageValues.class, Metadata.class,
				Name.class, PackageName.class, Packaging.class, PackagingValues.class, ProjectType.class,
				ProjectTypeValue.class, Version.class);
		registerForMostReflection(hints.reflection(), EncodingDetectingInputStream.class);
	}

	private void registerForMostReflection(ReflectionHints reflectionHints, Class<?>... classes) {
		for (Class<?> clazz : classes) {
			reflectionHints.registerType(clazz, hint -> {
				hint.withMembers(
						MemberCategory.DECLARED_CLASSES, MemberCategory.DECLARED_FIELDS,
						MemberCategory.PUBLIC_CLASSES, MemberCategory.PUBLIC_FIELDS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INTROSPECT_DECLARED_METHODS,
						MemberCategory.INVOKE_DECLARED_METHODS);
			});
		}
	}

	private void registerForMostReflection(ReflectionHints reflectionHints, String... classNames) {
		reflectionHints.registerTypes(typeReferences(classNames),
				hint -> {
					hint.withMembers(
							MemberCategory.DECLARED_CLASSES, MemberCategory.DECLARED_FIELDS,
							MemberCategory.PUBLIC_CLASSES, MemberCategory.PUBLIC_FIELDS,
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INTROSPECT_DECLARED_METHODS,
							MemberCategory.INVOKE_DECLARED_METHODS);
				});
	}

	private List<TypeReference> typeReferences(String... classNames) {
		return Stream.of(classNames).map(TypeReference::of).collect(Collectors.toList());
	}

}

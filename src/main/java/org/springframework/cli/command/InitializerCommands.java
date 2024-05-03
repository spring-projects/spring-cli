/*
 * Copyright 2022-2024 the original author or authors.
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

package org.springframework.cli.command;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import org.springframework.cli.config.SpringCliProperties;
import org.springframework.cli.initializr.InitializrClient;
import org.springframework.cli.initializr.InitializrClientCache;
import org.springframework.cli.initializr.InitializrUtils;
import org.springframework.cli.initializr.model.Metadata;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.component.context.ComponentContext;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.util.ObjectUtils;

/**
 * Commands for spring initializr.
 *
 * @author Janne Valkealahti
 */
@Command(command = "boot", group = "Boot")
public class InitializerCommands extends AbstractShellComponent {

	private static final String PATH_NAME = "Path";

	private static final String PATH_ID = "path";

	private static final String PROJECT_NAME = "Project";

	private static final String PROJECT_ID = "project";

	private static final String LANGUAGE_NAME = "Language";

	private static final String LANGUAGE_ID = "language";

	private static final String BOOT_VERSION_NAME = "Spring Boot";

	private static final String BOOT_VERSION_ID = "bootVersion";

	private static final String VERSION_NAME = "Version";

	private static final String VERSION_ID = "version";

	private static final String GROUP_NAME = "Group";

	private static final String GROUP_ID = "group";

	private static final String ARTIFACT_NAME = "Artifact";

	private static final String ARTIFACT_ID = "artifact";

	private static final String NAME_NAME = "Name";

	private static final String NAME_ID = "name";

	private static final String DESCRIPTION_NAME = "Description";

	private static final String DESCRIPTION_ID = "description";

	private static final String PACKAGE_NAME_NAME = "Package Name";

	private static final String PACKAGE_NAME_ID = "packageName";

	private static final String DEPENDENCIES_NAME = "Dependencies";

	private static final String DEPENDENCIES_ID = "dependencies";

	private static final String PACKAGING_NAME = "Packaging";

	private static final String PACKAGING_ID = "packaging";

	private static final String JAVA_VERSION_NAME = "Java";

	private static final String JAVA_VERSION_ID = "javaVersion";

	private static final Comparator<SelectorItem<String>> NAME_COMPARATOR = (o1, o2) -> {
		return o1.getName().compareTo(o2.getName());
	};

	private static final Comparator<SelectorItem<String>> JAVA_VERSION_COMPARATOR = (o1, o2) -> {
		try {
			Integer oo1 = Integer.valueOf(o1.getName());
			Integer oo2 = Integer.valueOf(o2.getName());
			return oo1.compareTo(oo2);
		}
		catch (Exception ex) {
		}
		return NAME_COMPARATOR.compare(o1, o2);
	};

	private final InitializrClientCache clientCache;

	private final ComponentFlow.Builder componentFlowBuilder;

	private final SpringCliProperties springCliProperties;

	InitializerCommands(InitializrClientCache clientCache, ComponentFlow.Builder componentFlowBuilder,
			SpringCliProperties springCliProperties) {
		this.clientCache = clientCache;
		this.componentFlowBuilder = componentFlowBuilder;
		this.springCliProperties = springCliProperties;
	}

	@Command(command = "start", description = "Create a new project from start.spring.io")
	public String init(@Option(description = "Path to extract") String path,
			@Option(description = "Project") String project, @Option(description = "Language") String language,
			@Option(longNames = "boot-version", description = "Language") String bootVersion,
			@Option(description = "Version") String version, @Option(description = "Group") String group,
			@Option(description = "Artifact") String artifact, @Option(description = "Name") String name,
			@Option(description = "Description") String description,
			@Option(longNames = "package-name", description = "Package Name") String packageName,
			@Option(description = "Dependencies") List<String> dependencies,
			@Option(description = "Packaging") String packaging,
			@Option(longNames = "java-version", description = "Java") String javaVersion) {
		InitializrClient client = buildClient();
		Metadata metadata = client.getMetadata();

		Map<String, String> projectSelectItems = metadata.getType()
			.getValues()
			.stream()
			.filter(v -> ObjectUtils.nullSafeEquals(v.getTags().get("format"), "project"))
			.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));

		String defaultProject = metadata.getType().getDefault();
		String defaultProjectSelect = projectSelectItems.entrySet()
			.stream()
			.filter(e -> ObjectUtils.nullSafeEquals(e.getValue(), defaultProject))
			.map(e -> e.getKey())
			.findFirst()
			.orElse(null);
		Map<String, String> languageSelectItems = metadata.getLanguage()
			.getValues()
			.stream()
			.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));

		String defaultLanguage = metadata.getLanguage().getDefault();
		String defaultLanguageSelect = languageSelectItems.entrySet()
			.stream()
			.filter(e -> ObjectUtils.nullSafeEquals(e.getValue(), defaultLanguage))
			.map(e -> e.getKey())
			.findFirst()
			.orElse(null);

		Map<String, String> bootSelectItems = metadata.getBootVersion()
			.getValues()
			.stream()
			.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		String defaultBootVersion = metadata.getBootVersion().getDefaultversion();
		String defaultBootVersionSelect = bootSelectItems.entrySet()
			.stream()
			.filter(e -> ObjectUtils.nullSafeEquals(e.getValue(), defaultBootVersion))
			.map(e -> e.getKey())
			.findFirst()
			.orElse(null);

		String defaultVersion = metadata.getVersion().getDefault();
		String defaultGroupId = metadata.getGroupId().getDefault();
		String defaultArtifact = metadata.getArtifactId().getDefault();
		String defaultName = metadata.getName().getDefault();
		String defaultDescription = metadata.getDescription().getDefault();
		String defaultPackageName = metadata.getPackageName().getDefault();
		dependencies = (dependencies != null) ? dependencies : Collections.emptyList();

		Map<String, String> packagingSelectItems = metadata.getPackaging()
			.getValues()
			.stream()
			.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		String defaultPackaging = metadata.getPackaging().getDefault();
		String defaultPackagingSelect = packagingSelectItems.entrySet()
			.stream()
			.filter(e -> ObjectUtils.nullSafeEquals(e.getValue(), defaultPackaging))
			.map(e -> e.getKey())
			.findFirst()
			.orElse(null);

		Map<String, String> javaVersionSelectItems = metadata.getJavaVersion()
			.getValues()
			.stream()
			.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		String defaultJavaVersion = metadata.getJavaVersion().getDefault();
		String defaultJavaVersionSelect = javaVersionSelectItems.entrySet()
			.stream()
			.filter(e -> ObjectUtils.nullSafeEquals(e.getValue(), defaultJavaVersion))
			.map(e -> e.getKey())
			.findFirst()
			.orElse(null);

		// @formatter:off
		ComponentFlow wizard = componentFlowBuilder.clone().reset()
				.withPathInput(PATH_ID)
					.name(PATH_NAME)
					.resultValue(path)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withSingleItemSelector(PROJECT_ID)
					.name(PROJECT_NAME)
					.resultValue(project)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(projectSelectItems)
					.defaultSelect(defaultProjectSelect)
					.sort(NAME_COMPARATOR)
					.and()
				.withSingleItemSelector(LANGUAGE_ID)
					.name(LANGUAGE_NAME)
					.resultValue(language)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(languageSelectItems)
					.defaultSelect(defaultLanguageSelect)
					.sort(NAME_COMPARATOR)
					.and()
				.withSingleItemSelector(BOOT_VERSION_ID)
					.name(BOOT_VERSION_NAME)
					.resultValue(bootVersion)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(bootSelectItems)
					.defaultSelect(defaultBootVersionSelect)
					.sort(NAME_COMPARATOR.reversed())
					.and()
				.withStringInput(VERSION_ID)
					.name(VERSION_NAME)
					.defaultValue(defaultVersion)
					.resultValue(version)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withStringInput(GROUP_ID)
					.name(GROUP_NAME)
					.defaultValue(defaultGroupId)
					.resultValue(group)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withStringInput(ARTIFACT_ID)
					.name(ARTIFACT_NAME)
					.defaultValue(defaultArtifact)
					.resultValue(artifact)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withStringInput(NAME_ID)
					.name(NAME_NAME)
					.defaultValue(defaultName)
					.resultValue(name)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withStringInput(DESCRIPTION_ID)
					.name(DESCRIPTION_NAME)
					.defaultValue(defaultDescription)
					.resultValue(description)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withStringInput(PACKAGE_NAME_ID)
					.name(PACKAGE_NAME_NAME)
					.defaultValue(defaultPackageName)
					.resultValue(packageName)
					.resultMode(ResultMode.ACCEPT)
					.and()
				.withMultiItemSelector(DEPENDENCIES_ID)
					.name(DEPENDENCIES_NAME)
					.resultValues(dependencies)
					.resultMode(ResultMode.ACCEPT)
					.preHandler(context -> {
						String bootVersionValue = context.get(BOOT_VERSION_ID);
						List<SelectItem> dependenciesSelectItems = metadata.getDependencies().getValues().stream()
								.flatMap(dc -> dc.getValues().stream())
								.map(dep -> SelectItem.of(dep.getName(), dep.getId(), InitializrUtils.isDependencyCompatible(dep, bootVersionValue), false))
								.collect(Collectors.toList());
						List<SelectorItem<String>> selectorItems = dependenciesSelectItems.stream()
								.map(si -> SelectorItem.of(si.name(), si.item(), si.enabled(), false))
								.collect(Collectors.toList());
						context.setItems(selectorItems);
					})
					.sort(NAME_COMPARATOR)
					.max(7)
					.and()
				.withSingleItemSelector(PACKAGING_ID)
					.name(PACKAGING_NAME)
					.resultValue(packaging)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(packagingSelectItems)
					.defaultSelect(defaultPackagingSelect)
					.sort(NAME_COMPARATOR)
					.and()
				.withSingleItemSelector(JAVA_VERSION_ID)
					.name(JAVA_VERSION_NAME)
					.resultValue(javaVersion)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(javaVersionSelectItems)
					.defaultSelect(defaultJavaVersionSelect)
					.sort(JAVA_VERSION_COMPARATOR)
					.and()
				.build();
		// @formatter:on

		ComponentFlowResult result = wizard.run();
		ComponentContext<?> context = result.getContext();

		Path pathValue = result.getContext().get(PATH_ID);
		List<String> dependenciesValue = result.getContext().get(DEPENDENCIES_ID);
		Path generated = client.generate(context.get(PROJECT_ID, String.class), context.get(LANGUAGE_ID, String.class),
				context.get(BOOT_VERSION_ID, String.class), dependenciesValue, context.get(VERSION_ID, String.class),
				context.get(GROUP_ID, String.class), context.get(ARTIFACT_ID, String.class),
				context.get(NAME_ID, String.class), context.get(DESCRIPTION_ID, String.class),
				context.get(PACKAGE_NAME_ID, String.class), context.get(PACKAGING_ID, String.class),
				context.get(JAVA_VERSION_ID, String.class));

		File outFile = pathValue.toFile();
		if (!outFile.mkdirs()) {
			throw new RuntimeException(String.format("Can't create path %s", outFile.getAbsolutePath()));
		}
		Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
		try {
			archiver.extract(generated.toFile(), outFile);
		}
		catch (Exception ex) {
			throw new RuntimeException(String.format("Extraction error from %s to %s",
					generated.toFile().getAbsolutePath(), outFile.getAbsolutePath()), ex);
		}
		return String.format("Extracted to %s", outFile.getAbsolutePath());
	}

	private InitializrClient buildClient() {
		String cacheKey = this.springCliProperties.getInitializr().getBaseUrl();
		return clientCache.get(cacheKey);
	}

}

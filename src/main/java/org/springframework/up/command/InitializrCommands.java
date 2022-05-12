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
package org.springframework.up.command;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.component.context.ComponentContext;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.up.initializr.InitializrClient;
import org.springframework.up.initializr.InitializrUtils;
import org.springframework.up.initializr.model.Metadata;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Commands for spring initializr.
 *
 * @author Janne Valkealahti
 */
@ShellComponent
public class InitializrCommands extends AbstractShellComponent {

	private final static String PATH_NAME = "Path";
	private final static String PATH_ID = "path";
	private final static String PROJECT_NAME = "Project";
	private final static String PROJECT_ID = "project";
	private final static String LANGUAGE_NAME = "Language";
	private final static String LANGUAGE_ID = "language";
	private final static String BOOT_VERSION_NAME = "Spring Boot";
	private final static String BOOT_VERSION_ID = "bootVersion";
	private final static String VERSION_NAME = "Version";
	private final static String VERSION_ID = "version";
	private final static String GROUP_NAME = "Group";
	private final static String GROUP_ID = "group";
	private final static String ARTIFACT_NAME = "Artifact";
	private final static String ARTIFACT_ID = "artifact";
	private final static String NAME_NAME = "Name";
	private final static String NAME_ID = "name";
	private final static String DESCRIPTION_NAME = "Description";
	private final static String DESCRIPTION_ID = "description";
	private final static String PACKAGE_NAME_NAME = "Package Name";
	private final static String PACKAGE_NAME_ID = "packageName";
	private final static String DEPENDENCIES_NAME = "Dependencies";
	private final static String DEPENDENCIES_ID = "dependencies";
	private final static String PACKAGING_NAME = "Packaging";
	private final static String PACKAGING_ID = "packaging";
	private final static String JAVA_VERSION_NAME = "Java";
	private final static String JAVA_VERSION_ID = "javaVersion";

	private final static Comparator<SelectorItem<String>> NAME_COMPARATOR = (o1, o2) -> {
		return o1.getName().compareTo(o2.getName());
	};

	private final static Comparator<SelectorItem<String>> JAVA_VERSION_COMPARATOR = (o1, o2) -> {
		try {
			Integer oo1 = Integer.valueOf(o1.getName());
			Integer oo2 = Integer.valueOf(o2.getName());
			return oo1.compareTo(oo2);
		} catch (Exception e) {
		}
		return NAME_COMPARATOR.compare(o1, o2);
	};

	@Autowired
	private InitializrClient client;

	@Autowired
	private ComponentFlow.Builder componentFlowBuilder;

	@ShellMethod(key = "initializr new", value = "Create a new project from start.spring.io")
	public String init(
		@ShellOption(help = "Path to extract", defaultValue = ShellOption.NULL) String path,
		@ShellOption(help = "Project", defaultValue = ShellOption.NULL) String project,
		@ShellOption(help = "Language", defaultValue = ShellOption.NULL) String language,
		@ShellOption(help = "Language", defaultValue = ShellOption.NULL) String bootVersion,
		@ShellOption(help = "Version", defaultValue = ShellOption.NULL) String version,
		@ShellOption(help = "Group", defaultValue = ShellOption.NULL) String group,
		@ShellOption(help = "Artifact", defaultValue = ShellOption.NULL) String artifact,
		@ShellOption(help = "Name", defaultValue = ShellOption.NULL) String name,
		@ShellOption(help = "Description", defaultValue = ShellOption.NULL) String description,
		@ShellOption(help = "Package Name", defaultValue = ShellOption.NULL) String packageName,
		@ShellOption(help = "Dependencies", defaultValue = ShellOption.NULL) List<String> dependencies,
		@ShellOption(help = "Packaging", defaultValue = ShellOption.NULL) String packaging,
		@ShellOption(help = "Java", defaultValue = ShellOption.NULL) String javaVersion
	) {
		Metadata metadata = client.getMetadata();

		Map<String, String> projectSelectItems = metadata.getType().getValues().stream()
				.filter(v -> ObjectUtils.nullSafeEquals(v.getTags().get("format"), "project"))
				.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		Map<String, String> languageSelectItems = metadata.getLanguage().getValues().stream()
				.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		Map<String, String> bootSelectItems = metadata.getBootVersion().getValues().stream()
				.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		String defaultVersion = metadata.getVersion().getDefault();
		String defaultGroupId = metadata.getGroupId().getDefault();
		String defaultArtifact = metadata.getArtifactId().getDefault();
		String defaultName = metadata.getName().getDefault();
		String defaultDescription = metadata.getDescription().getDefault();
		String defaultPackageName = metadata.getPackageName().getDefault();
		dependencies = dependencies == null ? Collections.emptyList() : dependencies;
		Map<String, String> packagingSelectItems = metadata.getPackaging().getValues().stream()
				.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));
		Map<String, String> javaVersionSelectItems = metadata.getJavaVersion().getValues().stream()
				.collect(Collectors.toMap(v -> v.getName(), v -> v.getId()));

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
					.sort(NAME_COMPARATOR)
					.and()
				.withSingleItemSelector(LANGUAGE_ID)
					.name(LANGUAGE_NAME)
					.resultValue(language)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(languageSelectItems)
					.sort(NAME_COMPARATOR)
					.and()
				.withSingleItemSelector(BOOT_VERSION_ID)
					.name(BOOT_VERSION_NAME)
					.resultValue(bootVersion)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(bootSelectItems)
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
								.map(dep -> SelectItem.of(dep.getName(), dep.getId(), InitializrUtils.isDependencyCompatible(dep, bootVersionValue)))
								.collect(Collectors.toList());
						List<SelectorItem<String>> selectorItems = dependenciesSelectItems.stream()
								.map(si -> SelectorItem.of(si.name(), si.item(), si.enabled()))
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
					.sort(NAME_COMPARATOR)
					.and()
				.withSingleItemSelector(JAVA_VERSION_ID)
					.name(JAVA_VERSION_NAME)
					.resultValue(javaVersion)
					.resultMode(ResultMode.ACCEPT)
					.selectItems(javaVersionSelectItems)
					.sort(JAVA_VERSION_COMPARATOR)
					.and()
				.build();

		ComponentFlowResult result = wizard.run();
		ComponentContext<?> context = result.getContext();

		Path pathValue = result.getContext().get(PATH_ID);
		List<String> dependenciesValue = result.getContext().get(DEPENDENCIES_ID);
		Path generated = client.generate(context.get(PROJECT_ID, String.class),
				context.get(LANGUAGE_ID, String.class),
				context.get(BOOT_VERSION_ID, String.class),
				dependenciesValue,
				context.get(VERSION_ID, String.class),
				context.get(GROUP_ID, String.class),
				context.get(ARTIFACT_ID, String.class),
				context.get(NAME_ID, String.class),
				context.get(DESCRIPTION_ID, String.class),
				context.get(PACKAGE_NAME_ID, String.class),
				context.get(PACKAGING_ID, String.class),
				context.get(JAVA_VERSION_ID, String.class));

		File outFile = pathValue.toFile();
		if (!outFile.mkdirs()) {
			throw new RuntimeException(String.format("Can't create path %s", outFile.getAbsolutePath()));
		}
		Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
		try {
			archiver.extract(generated.toFile(), outFile);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Extraction error from %s to %s",
					generated.toFile().getAbsolutePath(), outFile.getAbsolutePath()), e);
		}
		return String.format("Extracted to %s", outFile.getAbsolutePath());
	}

	@ShellMethod(key = "initializr info", value = "Show the Initializr server being used")
	public String info() {
		return client.info();
	}

	@ShellMethod(key = "initializr dependencies", value = "List supported dependencies")
	public Table dependencies(
		@ShellOption(help = "Search string to limit results", defaultValue = ShellOption.NULL) String search,
		@ShellOption(help = "Limit to compatibility version", defaultValue = ShellOption.NULL) String version
	) {
		Metadata metadata = client.getMetadata();

		Stream<String[]> header = Stream.<String[]>of(new String[] { "Id", "Name", "Description", "Required version" });
		Stream<String[]> rows = metadata.getDependencies().getValues().stream()
				.flatMap(dc -> dc.getValues().stream())
				.filter(d -> InitializrUtils.isDependencyCompatible(d, version))
				.map(d -> new String[] { d.getId(), d.getName(), d.getDescription(), d.getVersionRange() })
				.filter(d -> matches(d, search));
		String[][] data = Stream.concat(header, rows).toArray(String[][]::new);

		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	private static boolean matches(String[] array, String search) {
		if (!StringUtils.hasText(search)) {
			return true;
		}
		search = search.toLowerCase();
		for (String field : array) {
			if (StringUtils.hasText(field) && field.toLowerCase().contains(search)) {
				return true;
			}
		}
		return false;
	}
}

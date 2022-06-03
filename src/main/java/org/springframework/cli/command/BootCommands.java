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
package org.springframework.cli.command;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.tika.Tika;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.merger.ProjectMerger;
import org.springframework.cli.support.AbstractSpringCliCommands;
import org.springframework.cli.support.SpringCliUserConfig;
import org.springframework.cli.support.SpringCliUserConfig.CommandDefaults;
import org.springframework.cli.support.SpringCliUserConfig.ProjectCatalog;
import org.springframework.cli.support.SpringCliUserConfig.ProjectRepositories;
import org.springframework.cli.support.SpringCliUserConfig.ProjectRepository;
import org.springframework.cli.support.configfile.YamlConfigFile;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.PackageNameUtils;
import org.springframework.cli.util.PomReader;
import org.springframework.cli.util.ProjectInfo;
import org.springframework.cli.util.RootPackageFinder;
import org.springframework.lang.Nullable;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import static org.springframework.cli.util.RefactorUtils.refactorPackage;

@ShellComponent
public class BootCommands extends AbstractSpringCliCommands {

	private static final Logger logger = LoggerFactory.getLogger(BootCommands.class);

	private static final String FALLBACK_DEFAULT_REPO_URL = "https://github.com/rd-1-2022/rpt-rest-service";

	private static final String FALLBACK_DEFAULT_PROJECT_NAME = "demo";

	private static final String FALLBACK_DEFAULT_PACKAGE_NAME = "com.example";
	private SpringCliUserConfig springCliUserConfig;

	private final SourceRepositoryService sourceRepositoryService;

	@Autowired
	public BootCommands(SpringCliUserConfig springCliUserConfig,
			SourceRepositoryService sourceRepositoryService) {
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
	}

	@ShellMethod(key = "boot new", value = "Create a new Spring Boot project from an existing project")
	public void bootNew(
			@ShellOption(help = "Create project from existing project name or URL", defaultValue = ShellOption.NULL, arity = 1) String from,
			@ShellOption(help = "Name of the new project", defaultValue = ShellOption.NULL, arity = 1) String name,
			@ShellOption(help = "Package name for the new project", defaultValue = FALLBACK_DEFAULT_PACKAGE_NAME, arity = 1) String packageName) {

		String urlToUse = !StringUtils.hasText(from) ? FALLBACK_DEFAULT_REPO_URL : getProjectRepositoryUrl(from);  // Will return string or throw exception
		String projectNameToUse = getProjectName("boot", "new", name); // Will return string, never null

		String packageNameToUse = getPackageName("boot", "new", packageName); // Will return string, never null

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.YELLOW));
		sb.append("Using project name " + projectNameToUse);
		sb.append(System.lineSeparator());
		sb.append("Using package name " + packageNameToUse);
		sb.append(System.lineSeparator());
		sb.append("Cloning project from " + urlToUse);
		sb.append(System.lineSeparator());

		shellPrint(sb.toAttributedString());

		// Fail fast if there is already a directory with the project name
		getProjectDirectoryFromProjectName(projectNameToUse);


		generateFromUrl(projectNameToUse, urlToUse, packageNameToUse);
	}

	private Path getProjectDirectoryFromProjectName(String projectNameToUse) {
		Path workingPath = IoUtils.getWorkingDirectory();
		Path projectDirectoryPath = Paths.get(workingPath.toString(), projectNameToUse);
		if (Files.exists(projectDirectoryPath) && Files.isDirectory(projectDirectoryPath)) {
			throw new SpringCliException("Directory named " + projectNameToUse + " already exists.  Choose another name.");
		}
		return projectDirectoryPath;
	}

	@ShellMethod(key = "boot add", value = "Merge an existing project into the current Spring Boot project")
	public void bootAdd(@ShellOption(help = "Add to project from an existing project name or URL", arity = 1) String from ) throws IOException {
		String urlToUse = getProjectRepositoryUrl(from);  // Will return string or throw exception
		String projectName = getProjectNameForAdd(from);  // Will return string
		Path repositoryContentsPath = sourceRepositoryService.retrieveRepositoryContents(urlToUse);
		ProjectMerger projectMerger = new ProjectMerger(repositoryContentsPath, IoUtils.getWorkingDirectory(), projectName);
		projectMerger.merge();
		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.GREEN));
		sb.append(System.lineSeparator());
		sb.append("--- Done! ---");
		shellPrint(sb.toAttributedString());
	}

	private String getProjectName(String commandName, String subCommandName, String optionProjectNameValue) {
		if (StringUtils.hasText(optionProjectNameValue)) {
			return optionProjectNameValue;
		}
		CommandDefaults commandDefaults = this.springCliUserConfig.getCommandDefaults();
		Optional<String> newProjectName = commandDefaults.findDefaultOptionValue(commandName, subCommandName, "name");
		if (newProjectName.isPresent()) {
			return newProjectName.get().replaceAll(" ", "_");
		} else {
			return (FALLBACK_DEFAULT_PROJECT_NAME);
		}
	}


	private String getProjectNameForAdd(String from) throws MalformedURLException {
		// Check it if is a URL, then use just the last part of the name as the 'project name'
		if (from.startsWith("https:")) {
			URL url = new URL(from);
			return new File(url.getPath()).getName();
		}

		// We don't have a URL, but a name, let's check that it can be resolved to a URL
		findUrlFromProjectName(from);
		// The name is valid, return name
		return from;
	}
	@Nullable
	private String getProjectRepositoryUrl(String from) {
		// Check it if is a URL
		if (from.startsWith("https:")) {
			return from;
		} else {
			// look up url based on name
			return findUrlFromProjectName(from);
		}
	}

	private String getPackageName(String commandName, String subCommandName, String packageName) {
		// If user provided, make sure it is sanitized
		if (packageName != null && !packageName.equals(FALLBACK_DEFAULT_PACKAGE_NAME) && StringUtils.hasText(packageName)) {
			return PackageNameUtils.getTargetPackageName(packageName, packageName);
		}
		CommandDefaults commandDefaults = this.springCliUserConfig.getCommandDefaults();
		Optional<String> newPackageName = commandDefaults.findDefaultOptionValue(commandName, subCommandName, "package-name");
		return newPackageName.orElse(FALLBACK_DEFAULT_PACKAGE_NAME);
	}

	@Nullable
	private String findUrlFromProjectName(String projectName) {
		Collection<ProjectRepository> projectRepositories = springCliUserConfig.getProjectRepositories().getProjectRepositories();
		if (projectRepositories != null) {
			String url = findUrlFromProjectRepositories(projectName, projectRepositories);
			if (url != null) return url;
		}

		List<ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
		if (projectCatalogs != null) {
			for (ProjectCatalog projectCatalog : projectCatalogs) {
				String url = projectCatalog.getUrl();
				Path path = sourceRepositoryService.retrieveRepositoryContents(url);
				YamlConfigFile yamlConfigFile = new YamlConfigFile();
				projectRepositories = yamlConfigFile.read(Paths.get(path.toString(), "project-repositories.yml"),
						ProjectRepositories.class).getProjectRepositories();
				url = findUrlFromProjectRepositories(projectName, projectRepositories);
				if (url != null) return url;
			}
		}

		throw new SpringCliException("Could not resolve project name " + projectName + " to URL.  Command `project list` shows available project names.");
	}

	@Nullable
	private String findUrlFromProjectRepositories(String projectName, Collection<ProjectRepository> projectRepositories) {
		for (ProjectRepository projectRepository : projectRepositories) {
			if (projectName.trim().equalsIgnoreCase(projectRepository.getName().trim())) {
				// match - get url
				String url = projectRepository.getUrl();
				if (StringUtils.hasText(url)) {
					return url;
				}
				break;
			}
		}
		return null;
	}

	private void generateFromUrl(String projectName, String url, String packageName) {

		logger.debug("Generating project {} from url {} with Java package name {} ", projectName, url, packageName);
		Path repositoryContentsPath = sourceRepositoryService.retrieveRepositoryContents(url);

		// Get existing package name
		Optional<String> existingPackageName = this.getRootPackageName(repositoryContentsPath);

		// Refactor packages if package name is available
		if (StringUtils.hasText(packageName) && existingPackageName.isPresent()) {
			refactorPackage(packageName, existingPackageName.get(), repositoryContentsPath);
		}

		// Derive existing project name
		Optional<ProjectInfo> projectInfo = getProjectInfo(repositoryContentsPath);
		logger.debug("Existing project = " + projectInfo);

		// Copy files
		File fromDir = repositoryContentsPath.toFile();
		File toDir = createProjectDirectory(projectName).toFile();

		DirectoryScanner ds = new DirectoryScanner();
		ds.setBasedir(fromDir);
		ds.scan();
		String[] fileNames = ds.getIncludedFiles();

		toDir.mkdirs();
		for (String fileName : fileNames) {
			File srcFile = new File(fromDir, fileName);
			File destFile = new File(toDir, fileName);
			logger.debug("Copy from " + srcFile + " to " + destFile);
			Tika tika = new Tika();
			try {
				FileUtils.getFileUtils().copyFile(srcFile, destFile);
				if (projectInfo.isPresent()) {
					if (tika.detect(destFile).startsWith("text") || tika.detect(destFile).contains("xml")) {
						List<String> replacedLines = new ArrayList<>();
						List<String> originalLines = Files.readAllLines(destFile.toPath());
						for (String originalLine : originalLines) {
							replaceString(projectName, projectInfo, destFile, replacedLines, originalLine);
						}
						Files.write(destFile.toPath(), replacedLines);
					}
					// set executable file system permissions if needed.
					if (srcFile.canExecute()) {
						destFile.setExecutable(true);
					}
				}
			} catch (IOException e) {
				throw new SpringCliException(
						"Could not copy files from " + fromDir.getAbsolutePath() + " to " + toDir.getAbsolutePath());
			}
		}

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.GREEN));
		sb.append("Project " + projectName + " created in directory " + toDir.getName());
		shellPrint(sb.toAttributedString());
	}

	private void replaceString(String projectName, Optional<ProjectInfo> projectInfo, File destFile, List<String> replacedLines, String originalLine) {
		boolean replaced = false;
		if (originalLine.contains(projectInfo.get().getName())) {
			replaced = true;
			// can only replace one token per line with this algorithm
			String processedLine = originalLine.replace(projectInfo.get().getName(), projectName);
			replacedLines.add(processedLine);
			logger.debug("In file " + destFile.getAbsolutePath() + " replaced " + projectInfo.get().getName() + " with "
					+ projectName);
			logger.debug("Processed line = " + processedLine);
		}
		if (!replaced) {
			replacedLines.add(originalLine);
		}
	}

	private Optional<ProjectInfo> getProjectInfo(Path repositoryContentsPath) {
		File contentDirectory = repositoryContentsPath.toFile();
		File pomFile = new File(contentDirectory, "pom.xml");
		if (pomFile.exists()) {
			PomReader pomReader = new PomReader();
			Model model = pomReader.readPom(pomFile);
			ProjectInfo projectInfo = new ProjectInfo(model.getName(), model.getGroupId(), model.getArtifactId(), model.getVersion());
			return Optional.of(projectInfo);
		}
		// TODO search settings.gradle
		return Optional.empty();
	}

	private Path createProjectDirectory(String projectName) {
		Path projectDirectory = getProjectDirectoryFromProjectName(projectName);
		IoUtils.createDirectory(projectDirectory);
		logger.debug("Created directory " + projectDirectory);
		return projectDirectory;
	}

	private Optional<String> getRootPackageName(Path workingPath) {
		// Derive fromPackage using location of existing @SpringBootApplication class.
		// TODO warning if find multiple @SpringBootApplication classes.
		RootPackageFinder rootPackageFinder = new RootPackageFinder();
		logger.debug("Looking for @SpringBootApplication in directory " + workingPath.toFile());
		Optional<String> rootPackage = rootPackageFinder.findRootPackage(workingPath.toFile());
		if (rootPackage.isEmpty()) {
			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.YELLOW));
			sb.append("Could find root package containing class with @SpringBootApplication.  No Java Package refactoring on the project will occur.");
			shellPrint(sb.toAttributedString());
			return Optional.empty();
		}

		return rootPackage;
	}




}

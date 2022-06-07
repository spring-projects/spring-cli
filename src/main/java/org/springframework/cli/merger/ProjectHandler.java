package org.springframework.cli.merger;

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

import org.springframework.cli.SpringCliException;
import org.springframework.cli.git.SourceRepositoryService;
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
import org.springframework.cli.util.RefactorUtils;
import org.springframework.cli.util.RootPackageFinder;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

/**
 * Contain features to create and modify projects. This is kept outside
 * of terminal classes to make things easier to test.
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 */
public class ProjectHandler {

	private static final Logger logger = LoggerFactory.getLogger(ProjectHandler.class);
	private static final String FALLBACK_DEFAULT_REPO_URL = "https://github.com/rd-1-2022/rpt-rest-service";
	private static final String FALLBACK_DEFAULT_PROJECT_NAME = "demo";
	private static final String FALLBACK_DEFAULT_PACKAGE_NAME = "com.example";

	private final SpringCliUserConfig springCliUserConfig;
	private final SourceRepositoryService sourceRepositoryService;
	private final TerminalMessage terminalMessage;

	/**
	 * Creates a project handler.
	 *
	 * @param springCliUserConfig the user config
	 * @param sourceRepositoryService the repo service
	 * @param terminalMessage the terminal message
	 */
	public ProjectHandler(SpringCliUserConfig springCliUserConfig, SourceRepositoryService sourceRepositoryService,
			TerminalMessage terminalMessage) {
		Assert.notNull(springCliUserConfig, "springCliUserConfig must be set");
		Assert.notNull(sourceRepositoryService, "sourceRepositoryService must be set");
		Assert.notNull(terminalMessage, "terminalMessage must be set");
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
		this.terminalMessage = terminalMessage;
	}

	/**
	 * Creates a project.
	 *
	 * @param from the from
	 * @param name the project name
	 * @param packageName the package name
	 * @param path the project path
	 */
	public void create(String from, String name, String packageName, String path) {

		// Will return string or throw exception
		String urlToUse = !StringUtils.hasText(from) ? FALLBACK_DEFAULT_REPO_URL : getProjectRepositoryUrl(from);
		// Will return string, never null
		String projectNameToUse = getProjectName("boot", "new", name);
		// Will return string, never null
		String packageNameToUse = getPackageName("boot", "new", packageName);

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.YELLOW));
		sb.append("Using project name " + projectNameToUse);
		sb.append(System.lineSeparator());
		sb.append("Using package name " + packageNameToUse);
		sb.append(System.lineSeparator());
		sb.append("Cloning project from " + urlToUse);
		sb.append(System.lineSeparator());
		this.terminalMessage.shellPrint(sb.toAttributedString());

		generateFromUrl(getProjectPath(path), projectNameToUse, urlToUse, packageNameToUse);
	}

	/**
	 * Adds and merges a project.
	 *
	 * @param from the from
	 * @param path the project path
	 */
	public void add(String from, String path) {
		// Will return string or throw exception
		String urlToUse = getProjectRepositoryUrl(from);
		// Will return string
		String projectName = getProjectNameForAdd(from);
		Path repositoryContentsPath = sourceRepositoryService.retrieveRepositoryContents(urlToUse);
		Path projectDir = getProjectPath(path);
		Path workingPath = projectDir != null ? projectDir : IoUtils.getWorkingDirectory();

		ProjectMerger projectMerger = new ProjectMerger(repositoryContentsPath, workingPath, projectName);
		projectMerger.merge();
		try {
			FileSystemUtils.deleteRecursively(repositoryContentsPath);
		} catch (IOException ex) {
			logger.warn("Could not delete path " + repositoryContentsPath, ex);
		}

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.GREEN));
		sb.append(System.lineSeparator());
		sb.append("--- Done! ---");
		this.terminalMessage.shellPrint(sb.toAttributedString());
	}

	private String getProjectNameForAdd(String from) {
		// Check it if is a URL, then use just the last part of the name as the 'project name'
		try {
			if (from.startsWith("https:")) {
				URL url = new URL(from);
				return new File(url.getPath()).getName();
			}
		} catch (MalformedURLException ex) {
			throw new SpringCliException("Malformed URL " + from, ex);
		}

		// We don't have a URL, but a name, let's check that it can be resolved to a URL
		findUrlFromProjectName(from);
		// The name is valid, return name
		return from;
	}

	private Path getProjectPath(String path) {
		Path resolved = null;
		if (StringUtils.hasText(path)) {
			resolved = Path.of(path);
		}
		return resolved;
	}

	private void replaceString(String projectName, Optional<ProjectInfo> projectInfo, File destFile,
			List<String> replacedLines, String originalLine) {
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

	private Optional<String> getRootPackageName(Path workingPath) {
		// Derive fromPackage using location of existing @SpringBootApplication class.
		// TODO warning if find multiple @SpringBootApplication classes.
		logger.debug("Looking for @SpringBootApplication in directory " + workingPath.toFile());
		Optional<String> rootPackage = RootPackageFinder.findRootPackage(workingPath.toFile());
		if (rootPackage.isEmpty()) {
			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.style(sb.style().foreground(AttributedStyle.YELLOW));
			sb.append("Could find root package containing class with @SpringBootApplication.  No Java Package refactoring on the project will occur.");
			this.terminalMessage.shellPrint(sb.toAttributedString());
			return Optional.empty();
		}

		return rootPackage;
	}

	private Optional<ProjectInfo> getProjectInfo(Path repositoryContentsPath) {
		File contentDirectory = repositoryContentsPath.toFile();
		File pomFile = new File(contentDirectory, "pom.xml");
		if (pomFile.exists()) {
			PomReader pomReader = new PomReader();
			Model model = pomReader.readPom(pomFile);
			ProjectInfo projectInfo = new ProjectInfo(model.getName(), model.getGroupId(), model.getArtifactId(),
					model.getVersion());
			return Optional.of(projectInfo);
		}
		// TODO search settings.gradle
		return Optional.empty();
	}

	private Path getProjectDirectoryFromProjectName(Path projectDir, String projectName) {
		Path workingPath = projectDir != null ? projectDir : IoUtils.getWorkingDirectory();
		Path projectDirectoryPath = Paths.get(workingPath.toString(), projectName);
		if (Files.exists(projectDirectoryPath) && Files.isDirectory(projectDirectoryPath)) {
			throw new SpringCliException(
					"Directory named " + projectName + " already exists.  Choose another name.");
		}
		return projectDirectoryPath;
	}

	private Path createProjectDirectory(Path projectDir, String projectName) {
		Path projectDirectory = getProjectDirectoryFromProjectName(projectDir, projectName);
		IoUtils.createDirectory(projectDirectory);
		logger.debug("Created directory " + projectDirectory);
		return projectDirectory;
	}

	private void generateFromUrl(Path projectDir, String projectName, String url, String packageName) {
		logger.debug("Generating project {} from url {} with Java package name {} ", projectName, url, packageName);
		File toDir = createProjectDirectory(projectDir, projectName).toFile();
		Path repositoryContentsPath = sourceRepositoryService.retrieveRepositoryContents(url);

		// Get existing package name
		Optional<String> existingPackageName = this.getRootPackageName(repositoryContentsPath);

		// Refactor packages if package name is available
		if (StringUtils.hasText(packageName) && existingPackageName.isPresent()) {
			RefactorUtils.refactorPackage(packageName, existingPackageName.get(), repositoryContentsPath);
		}

		// Derive existing project name
		Optional<ProjectInfo> projectInfo = getProjectInfo(repositoryContentsPath);
		logger.debug("Existing project = " + projectInfo);

		// Copy files
		File fromDir = repositoryContentsPath.toFile();

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
		try {
			FileSystemUtils.deleteRecursively(repositoryContentsPath);
		} catch (IOException ex) {
			logger.warn("Could not delete path " + repositoryContentsPath, ex);
		}

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(sb.style().foreground(AttributedStyle.GREEN));
		sb.append("Project " + projectName + " created in directory " + toDir.getName());
		this.terminalMessage.shellPrint(sb.toAttributedString());
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
			return FALLBACK_DEFAULT_PROJECT_NAME;
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
	private String findUrlFromProjectRepositories(String projectName,
			Collection<ProjectRepository> projectRepositories) {
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

	@Nullable
	private String findUrlFromProjectName(String projectName) {
		Collection<ProjectRepository> projectRepositories = springCliUserConfig.getProjectRepositories()
				.getProjectRepositories();
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
				try {
					FileSystemUtils.deleteRecursively(path);
				} catch (IOException ex) {
					logger.warn("Could not delete path " + path, ex);
				}
				url = findUrlFromProjectRepositories(projectName, projectRepositories);
				if (url != null) return url;
			}
		}

		throw new SpringCliException("Could not resolve project name " + projectName
				+ " to URL.  Command `project list` shows available project names.");
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

}

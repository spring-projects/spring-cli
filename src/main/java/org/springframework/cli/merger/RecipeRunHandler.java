/*
 * Copyright 2021 - 2023 the original author or authors.
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
package org.springframework.cli.merger;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.support.configfile.YamlConfigFile;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.lang.Nullable;
import org.springframework.rewrite.parsers.RewriteProjectParser;
import org.springframework.rewrite.parsers.RewriteProjectParsingResult;
import org.springframework.rewrite.project.resource.ProjectResourceSet;
import org.springframework.rewrite.project.resource.ProjectResourceSetFactory;
import org.springframework.rewrite.project.resource.ProjectResourceSetSerializer;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;


import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Fabian Krüger
 */
public class RecipeRunHandler {
    private static final Logger logger = LoggerFactory.getLogger(RecipeRunHandler.class);
    private final SpringCliUserConfig springCliUserConfig;
    private final SourceRepositoryService sourceRepositoryService;
    private final TerminalMessage terminalMessage;

    private final RewriteProjectParser parser;
    private final ProjectResourceSetFactory resourceSetFactory;
    private final ProjectResourceSetSerializer serializer;

    public RecipeRunHandler(SpringCliUserConfig springCliUserConfig, SourceRepositoryService sourceRepositoryService, TerminalMessage terminalMessage, RewriteProjectParser parser, ProjectResourceSetFactory resourceSetFactory, ProjectResourceSetSerializer serializer) {
        this.springCliUserConfig = springCliUserConfig;
        this.sourceRepositoryService = sourceRepositoryService;
        this.terminalMessage = terminalMessage;
        this.parser = parser;
        this.resourceSetFactory = resourceSetFactory;
        this.serializer = serializer;
    }

    public void run(String from, String path) {
        Path projectDir = IoUtils.getProjectPath(path);
        Path workingPath = projectDir != null ? projectDir : IoUtils.getWorkingDirectory();
        Path downloadDir = null;
        Path repositoryContentsPath = null;
        try {
            // Will return string or throw exception
            String urlToUse = getProjectRepositoryUrl(from);
//            repositoryContentsPath = sourceRepositoryService.retrieveRepositoryContents(urlToUse);
            // Will return string
            String projectName = getProjectNameUsingFrom(from);


//            sb.append("Getting project with URL " + urlToUse);
//            this.terminalMessage.print(sb.toAttributedString());


//        ProjectMerger projectMerger = new ProjectMerger(repositoryContentsPath, workingPath, projectName, this.terminalMessage);
//        projectMerger.merge();

            String repo = urlToUse;
            // Generate the JitPack URL for the repository

            String gitHubUsername = "fabapp2";
            String repositoryName = "spring-cli-command-playground";
            String version = "1.0-SNAPSHOT"; // Replace with the desired version

//            String jitpackUrl = String.format("https://jitpack.io/com/github/%s/%s/%s/%s-%s.jar",
//                    gitHubUsername, repositoryName, version, repositoryName, version);


//            String jitpackUrl = "https://jitpack.io/com/github/" + repo.substring(repo.lastIndexOf("/") + 1) + "/master-SNAPSHOT/" + repo.substring(repo.lastIndexOf("/") + 1) + "-master-SNAPSHOT.jar";

            String gitHash = "2c2653e";
            String myJitpackUrl = "https://jitpack.io/com/github/fabapp2/" + repositoryName + "/main-SNAPSHOT/" + repo.substring(repo.lastIndexOf("/") + 1) + "-" + gitHash + ".jar";

            downloadDir = Path.of(System.getProperty("java.io.tmpdir"));

            // Download the JAR file from JitPack
            String fileName = repo.substring(repo.lastIndexOf("/") + 1) + ".jar";

            String dir = downloadDir.resolve(fileName).toString();


            print("Using JitPack URL %s to create the jar".formatted(myJitpackUrl));

            DownloadUtils.downloadFile(myJitpackUrl, dir);
            // Add the downloaded JAR to the classpath
            Recipe recipe = getRecipe(dir);
            Path baseDir = Path.of(path);
            print("Parse project %s".formatted(path));
            RewriteProjectParsingResult lst = parser.parse(baseDir);
            ProjectResourceSet projectResourceSet = resourceSetFactory.create(baseDir, lst.sourceFiles());
            print("Executing recipe '%s' from repo %s to project %s".formatted(recipe.getName(), from, path));
            projectResourceSet.apply(recipe);
            serializer.writeChanges(projectResourceSet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


//        sb = new AttributedStringBuilder();
//        sb.style(sb.style().foreground(AttributedStyle.GREEN));
//        sb.append(System.lineSeparator());
//        sb.append("Done!");
//        terminalMessage.print(sb.toAttributedString());

//        try {
//            FileSystemUtils.deleteRecursively(downloadDir);
//        } catch (IOException ex) {
//            logger.warn("Could not delete path " + repositoryContentsPath, ex);
//        }

    }

    private void print(String message) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(sb.style().foreground(AttributedStyle.WHITE));
        sb.append(message);
        this.terminalMessage.print(sb.toAttributedString());
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

    private static Recipe getRecipe(String jarFilePath) throws Exception {
        File jarFile = new File(jarFilePath);
        URL jarUrl = jarFile.toURI().toURL();


        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});

        // The class could be provided ion some configuration or we can find it by analysing the inheritance hierarchy and find the class implememnting the API interface
        Class<?> aClass = classLoader.loadClass("com.example.MyCustomRecipeProvider");
        Object newInstance = aClass.getConstructor().newInstance();

        Method getMessage = aClass.getDeclaredMethod("getRecipe");
        Object returned = getMessage.invoke(newInstance);
        if (!Recipe.class.isInstance(returned)) {
            throw new IllegalArgumentException("The method does not return a Recipe. Type was: " + returned.getClass().getName());
        }
        Recipe recipe = (Recipe) returned;
        return recipe;
        //        System.out.println(invoke);

//        Class<?> loadedClass = Class.forName("com.example.MyMain");
//        Object instance = loadedClass.newInstance();

        // Create a new URLClassLoader with the JAR file
//        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//        Class<URLClassLoader> sysclass = URLClassLoader.class;
//
//        try {
//            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
//            method.setAccessible(true);
//            method.invoke(classLoader, jarUrl);
//        } catch (Throwable t) {
//            t.printStackTrace();
//            throw new IOException("Error, could not add URL to system classloader");
//        }
    }

    @Nullable
    private String findUrlFromProjectName(String projectName) {
        Collection<SpringCliUserConfig.ProjectRepository> projectRepositories = springCliUserConfig.getProjectRepositories()
                .getProjectRepositories();
        if (projectRepositories != null && projectRepositories.size() > 0) {
            String url = findUrlFromProjectRepositories(projectName, projectRepositories);
            if (url != null) return url;
        }

        List<SpringCliUserConfig.ProjectCatalog> projectCatalogs = springCliUserConfig.getProjectCatalogs().getProjectCatalogs();
        if (projectCatalogs != null) {
            for (SpringCliUserConfig.ProjectCatalog projectCatalog : projectCatalogs) {
                String url = projectCatalog.getUrl();
                Path path = sourceRepositoryService.retrieveRepositoryContents(url);
                YamlConfigFile yamlConfigFile = new YamlConfigFile();
                projectRepositories = yamlConfigFile.read(Paths.get(path.toString(), "project-catalog.yml"),
                        SpringCliUserConfig.ProjectRepositories.class).getProjectRepositories();
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
                + " to URL.  The command `project list` shows the available project names.");
    }

    private String getProjectNameUsingFrom(String from) {
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

    @Nullable
    private String findUrlFromProjectRepositories(String projectName,
                                                  Collection<SpringCliUserConfig.ProjectRepository> projectRepositories) {
        for (SpringCliUserConfig.ProjectRepository projectRepository : projectRepositories) {
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

    public class DownloadUtils {

        public static void downloadFile(String fileUrl, String destinationPath) throws IOException {
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(destinationPath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

}

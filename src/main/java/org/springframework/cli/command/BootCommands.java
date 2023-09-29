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
package org.springframework.cli.command;

import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.git.SourceRepositoryService;
import org.springframework.cli.merger.ProjectHandler;
import org.springframework.cli.util.ProjectInfo;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.sbm.parsers.*;
import org.springframework.sbm.parsers.events.FinishedParsingResourceEvent;
import org.springframework.sbm.project.resource.ProjectResourceSet;
import org.springframework.sbm.project.resource.ProjectResourceSetFactory;
import org.springframework.sbm.project.resource.ProjectResourceSetSerializer;
import org.springframework.sbm.project.resource.RewriteMigrationResultMerger;
import org.springframework.sbm.recipes.RewriteRecipeDiscovery;
import org.springframework.sbm.utils.OsAgnosticPathMatcher;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Command(command = "boot", group = "Boot")
public class BootCommands extends AbstractSpringCliCommands {

	private final SpringCliUserConfig springCliUserConfig;
	private final SourceRepositoryService sourceRepositoryService;

	private final TerminalMessage terminalMessage;

	@Autowired
	public BootCommands(SpringCliUserConfig springCliUserConfig,
                        SourceRepositoryService sourceRepositoryService,
                        TerminalMessage terminalMessage) {
		this.springCliUserConfig = springCliUserConfig;
		this.sourceRepositoryService = sourceRepositoryService;
		this.terminalMessage = terminalMessage;
	}

	@Command(command = "new", description = "Create a new Spring Boot project from an existing project")
	public void bootNew(
			@Option(description = "Name of the new project", required = true) String name,
			@Option(description = "Create project from existing project name or URL") String from,
			@Option(longNames = "group-id", description = "Group ID of the new project") String groupId,
			@Option(longNames = "artifact-id", description = "Artifact ID of the new project") String artifactId,
			@Option(description = "Version of the new project") String version,
			@Option(description = "Description of the new project") String description,
			@Option(longNames = "package-name", description = "Package name for the new project") String packageName,
			@Option(description = "Path to run the command in, most of the time this is not necessary to specify and the default value is the current working directory.") String path) {
		ProjectInfo projectInfo = new ProjectInfo(groupId, artifactId, version, name, description, packageName);
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.create(from, path, projectInfo);
	}


	@Command(command = "add", description = "Merge an existing project into the current Spring Boot project")
	public void bootAdd(
			@Option(description = "Add to project from an existing project name or URL") String from,
			@Option(description = "Path") String path) {
		ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
		handler.add(from, path);
	}


	@Autowired
	private RewriteProjectParser parser;
	@Autowired
	private RewriteRecipeDiscovery discovery;
	@Autowired
	private ProjectResourceSetFactory resourceSetFactory;
	@Autowired
	ProjectResourceSetSerializer serializer;

	@Command(command = "upgrade")
	public void bootUpgrade(@Option(description = "Path") String path) {

		// generate a path for win and nux
		Path baseDir = Path.of(".").toAbsolutePath().normalize();
		if(path != null) {
			baseDir = Path.of(path).toAbsolutePath().normalize();
		}

		// parse to AST
		RewriteProjectParsingResult parsingResult = parser.parse(baseDir);

		System.out.println("Parsed AST of %d resources".formatted(parsingResult.sourceFiles().size()));

		// discover recipe
		String recipeName = "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_1";
		List<Recipe> recipes = discovery.discoverRecipes();
		Optional<Recipe> recipe = recipes.stream().filter(r -> recipeName.equals(r.getName())).findFirst();

		if(recipe.isPresent()) {

			System.out.println("Found recipe %s".formatted(recipeName));

			// Use ProjectResourceSet abstraction
			ProjectResourceSet projectResourceSet = resourceSetFactory.create(baseDir, parsingResult.sourceFiles());

			// To apply recipes
			System.out.println("Applying recipe %s".formatted(recipeName));
			projectResourceSet.apply(recipe.get());

			// And synchronize changes with FS
			System.out.println("Applied recipe %s. Writing back changes.".formatted(recipeName));
			serializer.writeChanges(projectResourceSet);

		} else {
			System.err.println("Could not find recipe " + recipeName + ".");
		}
	}

}

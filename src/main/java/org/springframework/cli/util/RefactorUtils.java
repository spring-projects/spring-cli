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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.Java17Parser;
import org.openrewrite.java.JavaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;

public class RefactorUtils {

	private static final Logger logger = LoggerFactory.getLogger(RefactorUtils.class);

	public static void refactorPackage(String newPackage, String oldPackage, Path workingPath) {
		JavaParser javaParser = new Java17Parser.Builder().build();
		FileTypeCollectingFileVisitor collector = new FileTypeCollectingFileVisitor(".java");
		try {
			Files.walkFileTree(workingPath, collector);
		}
		catch (IOException e) {
			throw new SpringCliException("Failed reading files in " + workingPath, e);
		}
		Consumer<Throwable> onError = e -> {
			logger.error("error in javaParser execution", e);
		};
		InMemoryExecutionContext executionContext = new InMemoryExecutionContext(onError);
		List<Path> matches = collector.getMatches();
		List<? extends SourceFile> compilationUnits = javaParser.parse(matches, null, executionContext);
		ResultsExecutor container = new ResultsExecutor();

		ChangePackage recipe = new ChangePackage(oldPackage, newPackage, true);
		RecipeRun run = recipe.run(compilationUnits);
		List<Result> results = run.getResults();
		container.addAll(results);
		try {
			container.execute();
		}
		catch (IOException e) {
			throw new SpringCliException("Error performing refactoring", e);
		}

		//TODO change groupId and artifactId
	}
}

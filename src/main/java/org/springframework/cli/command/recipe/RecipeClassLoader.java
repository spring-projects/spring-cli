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
package org.springframework.cli.command.recipe;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.springframework.util.Assert;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Fabian Krüger
 */
public class RecipeClassLoader {

    public static Class<?> load(String clazzName, Set<Path> jars) throws RecipeLoadingException {
        Set<Path> missingJars = jars.stream().filter(p -> !Files.exists(p)).collect(Collectors.toSet());
        if (!missingJars.isEmpty()) {
            throw new RecipeLoadingException("JARs '%s' do not exist.".formatted(missingJars));
        }

        URL[] jarUrls = jars.stream()
                .map(Path::toUri)
                .map(toUrl())
                .toArray(URL[]::new);

        URLClassLoader classLoader = new URLClassLoader(jarUrls, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> loadedClass = null;

        try {
            loadedClass = classLoader.loadClass(clazzName);
            return loadedClass;
        } catch (ClassNotFoundException e) {
            throw new RecipeLoadingException("Recipe '%s' could not be found in provided jars: %s".formatted(clazzName, jars), e);
        }
    }

    @NotNull
    private static Function<URI, URL> toUrl() {
        return uri -> {
            try {
                return uri.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        };
    }
}

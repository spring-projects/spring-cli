/*
 * Copyright 2024. the original author or authors.
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
class RecipeClassLoaderTest {

    @Test
    @DisplayName("should load recipe from provided JAR")
    void shouldLoadClassFromProvidedJar() throws RecipeLoadingException {
        RecipeClassLoader sut = new RecipeClassLoader();
        Path jarPath = Path.of(".").resolve("test-data/jar/spring-rewrite-commons-recipes-0.1.0-SNAPSHOT-b966be1ac4-1.jar").toAbsolutePath().normalize();
        Set<Path> jars = Set.of(jarPath);
        String factoryClass = "org.springframework.rewrite.recipes.RecipeFactory";
        Class<?> recipe = sut.load(factoryClass, jars);
        assertThat(recipe).isNotNull();
    }
}
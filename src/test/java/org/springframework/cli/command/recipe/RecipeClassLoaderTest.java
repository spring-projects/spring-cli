package org.springframework.cli.command.recipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.DeclarativeRecipe;
import org.springframework.cli.command.recipe.RecipeClassLoader;

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
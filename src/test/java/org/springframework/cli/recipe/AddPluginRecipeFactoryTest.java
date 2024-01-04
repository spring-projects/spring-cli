package org.springframework.cli.recipe;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.maven.AddPlugin;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
class AddPluginRecipeFactoryTest {

    private AddPluginRecipeFactory sut = new AddPluginRecipeFactory();

    @Test
    @DisplayName("should create Recipe from valid XML")
    void shouldCreateRecipeFromValidXml() {
        @Language("xml")
        String snippet = """
                <plugin>
                    <groupId>groupId</groupId>
                    <artifactId>artifactId</artifactId>
                    <version>version</version>
                    <configuration>configuration</configuration>
                    <dependencies>dependencies</dependencies>
                    <executions>executions</executions>
                    <filePattern>filePattern</filePattern>
                </plugin>
                """;
        AddPlugin recipe = sut.create(snippet);
        assertThat(ReflectionTestUtils.getField(recipe, "groupId").toString()).isEqualTo("groupId");
        assertThat(ReflectionTestUtils.getField(recipe, "artifactId").toString()).isEqualTo("artifactId");
        assertThat(ReflectionTestUtils.getField(recipe, "version").toString()).isEqualTo("version");
        assertThat(ReflectionTestUtils.getField(recipe, "configuration").toString()).isEqualTo("configuration");
        assertThat(ReflectionTestUtils.getField(recipe, "dependencies").toString()).isEqualTo("dependencies");
        assertThat(ReflectionTestUtils.getField(recipe, "executions").toString()).isEqualTo("executions");
        assertThat(ReflectionTestUtils.getField(recipe, "filePattern").toString()).isEqualTo("filePattern");
    }

}
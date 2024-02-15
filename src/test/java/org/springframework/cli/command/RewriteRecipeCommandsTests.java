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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.FileExtensionUtils;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Fabian Krüger
 */
class RewriteRecipeCommandsTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MockConfigurations.MockBaseConfig.class);


    @Test
    @DisplayName("can run recipe from catalog")
    void canRunRecipeFromCatalog(final @TempDir Path workingDir) throws IOException {
        String recipeId = "AddLicenseHeader";
        String parameter = "licenseText=Copyright!";

        String path = workingDir.toString();
        Path javaMainDir = workingDir.resolve("src/main/java/");
        Files.createDirectories(javaMainDir);
        Path javaSourcePath = Files.writeString(javaMainDir.resolve("MyClass.java"),
                """
                public class MyClass {}
                """);
        Files.writeString(workingDir.resolve("pom.xml"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>artifact</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <properties>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                    </properties>
                </project>
                """
                );


        this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
            RewriteRecipeCommands sut = context.getBean(RewriteRecipeCommands.class);
            sut.apply(recipeId, parameter, path);
        });

        assertThat(Files.readString(javaSourcePath)).isEqualTo("""
                /*
                 * Copyright!
                 */
                public class MyClass {}
                """);
    }
}
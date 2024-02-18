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

package org.springframework.cli.merger;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.Assertions;

import org.springframework.cli.util.ProjectInfo;

public class ChangeNewlyClonedPomRecipeTests implements RewriteTest {

	@Test
	void updateAllFields() {
		rewriteRun(spec -> spec.recipe(new ChangeNewlyClonedPomRecipe(
				new ProjectInfo("groupId", "artifactId", "version", "projectName", "projectDescription", null))),
				Assertions.xml("""
						<project>
							<modelVersion>4.0.0</modelVersion>

							<groupId>com.example</groupId>
							<artifactId>rest-service-complete</artifactId>
							<version>0.0.1-SNAPSHOT</version>
							<name>rest-service</name>
							<description>Demo Rest Service project for Spring Boot</description>

							<dependencies>
								<dependency>
									<groupId>org.springframework.boot</groupId>
									<artifactId>spring-boot-starter-web</artifactId>
								</dependency>
							</dependencies>
						</project>
						""", """
						<project>
							<modelVersion>4.0.0</modelVersion>

							<groupId>groupId</groupId>
							<artifactId>artifactId</artifactId>
							<version>version</version>
							<name>projectName</name>
							<description>projectDescription</description>

							<dependencies>
								<dependency>
									<groupId>org.springframework.boot</groupId>
									<artifactId>spring-boot-starter-web</artifactId>
								</dependency>
							</dependencies>
						</project>
						"""));
	}

	@Test
	void updateWhenNoNameInOriginalPom() {
		rewriteRun(spec -> spec.recipe(new ChangeNewlyClonedPomRecipe(
				new ProjectInfo("groupId", "artifactId", "version", "projectName", "projectDescription", null))),
				Assertions.xml("""
						<project>
							<modelVersion>4.0.0</modelVersion>
							<groupId>com.xkcd</groupId>
							<artifactId>rest-service-complete</artifactId>
							<version>0.0.1-SNAPSHOT</version>
							<description>Demo Rest Service project for Spring Boot</description>

							<dependencies>
								<dependency>
									<groupId>org.springframework.boot</groupId>
									<artifactId>spring-boot-starter-web</artifactId>
								</dependency>
							</dependencies>
						</project>
						""", """
						<project>
							<modelVersion>4.0.0</modelVersion>
							<groupId>groupId</groupId>
							<artifactId>artifactId</artifactId>
							<version>version</version>
							<name>projectName</name>
							<description>projectDescription</description>

							<dependencies>
								<dependency>
									<groupId>org.springframework.boot</groupId>
									<artifactId>spring-boot-starter-web</artifactId>
								</dependency>
							</dependencies>
						</project>
						"""));
	}

}

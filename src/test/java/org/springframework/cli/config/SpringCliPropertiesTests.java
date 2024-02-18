/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cli.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCliPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void defaultNoPropertiesSet() {
		this.contextRunner.withUserConfiguration(Config1.class).run((context) -> {
			SpringCliProperties properties = context.getBean(SpringCliProperties.class);
			assertThat(properties.getInitializr().getBaseUrl()).isEqualTo("https://start.spring.io");
			assertThat(properties.getGithub().getClientId()).isNull();
			assertThat(properties.getGithub().getDefaultScopes()).isEqualTo("repo,read:org");
		});
	}

	@Test
	public void setProperties() {
		this.contextRunner.withPropertyValues("spring.cli.initializr.base-url=fakeurl")
			.withPropertyValues("spring.cli.github.client-id=fakeid")
			.withPropertyValues("spring.cli.github.default-scopes=fakescopes")
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				SpringCliProperties properties = context.getBean(SpringCliProperties.class);
				assertThat(properties.getInitializr().getBaseUrl()).isEqualTo("fakeurl");
				assertThat(properties.getGithub().getClientId()).isEqualTo("fakeid");
				assertThat(properties.getGithub().getDefaultScopes()).isEqualTo("fakescopes");
			});
	}

	@EnableConfigurationProperties({ SpringCliProperties.class })
	private static class Config1 {

	}

}

/*
 * Copyright 2022-2023 the original author or authors.
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

import java.time.Duration;
import java.util.Collection;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.jline.terminal.Terminal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.reactive.function.client.ReactorNettyHttpClientMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cli.initializr.InitializrClientCache;
import org.springframework.cli.runtime.command.DynamicMethodCommandResolver;
import org.springframework.cli.runtime.engine.model.MavenModelPopulator;
import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.runtime.engine.model.RootPackageModelPopulator;
import org.springframework.cli.runtime.engine.model.SystemModelPopulator;
import org.springframework.cli.util.SpringCliTerminal;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.result.CommandNotFoundMessageProvider;
import org.springframework.shell.style.ThemeResolver;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for cli related beans.
 *
 * @author Janne Valkealahti
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SpringCliProperties.class, SpringCliProjectCatalogProperties.class})
public class SpringCliConfiguration {

	@Bean
	public CommandNotFoundMessageProvider commandNotFoundMessageProvider() {
		return new SpringCliCommandNotFoundMessageProvider();
	}
	@Bean
	public CommandExceptionResolver commandExceptionResolver() {
		return new SpringCliExceptionResolver();
	}

	@Bean
	public SpringCliTerminal springCliTerminalMessage(Terminal terminal, ThemeResolver themeResolver) {
		return new SpringCliTerminal(terminal, themeResolver);
	}

	@Bean
	public ModelPopulator systemModelPopulator() {
		return new SystemModelPopulator();
	}

	@Bean
	public ModelPopulator mavenModelPopulator() {
		return new MavenModelPopulator();
	}

	@Bean
	public ModelPopulator rootPackageModelPopulator() {
		return new RootPackageModelPopulator();
	}

	@Bean
	public DynamicMethodCommandResolver dynamicMethodTargetRegistrar(Collection<ModelPopulator> modelPopulators,
			CommandRegistration.BuilderSupplier builder, TerminalMessage terminalMessage, ObjectProvider<Terminal> terminalProvider) {
		return new DynamicMethodCommandResolver(modelPopulators, builder, terminalMessage, terminalProvider);
	}

    @Bean
    public ReactorResourceFactory reactorClientResourceFactory() {
		// change default 2s quiet period so that context terminates more quick
        ReactorResourceFactory factory = new ReactorResourceFactory();
        factory.setShutdownQuietPeriod(Duration.ZERO);
        return factory;
    }

	@Bean
	ReactorNettyHttpClientMapper reactorNettyHttpClientMapper() {
        // workaround for native/graal issue
        // https://github.com/spring-projects-experimental/spring-native/issues/1319
		// There's also issue #4304 on https://github.com/oracle/graal
		return httpClient -> httpClient.resolver(DefaultAddressResolverGroup.INSTANCE);
	}

	@Bean
	InitializrClientCache initializrClientCache(WebClient.Builder webClientBuilder) {
		return new InitializrClientCache(webClientBuilder);
	}

	@Bean
	public SpringCliUserConfig springCliUserConfig() {
		return new SpringCliUserConfig();
	}

	@Bean
	public ProjectCatalogInitializer projectCatalogInitializer(SpringCliUserConfig springCliUserConfig,
			SpringCliProjectCatalogProperties springCliProjectCatalogProperties) {
		return new ProjectCatalogInitializer(springCliUserConfig, springCliProjectCatalogProperties);
	}
}

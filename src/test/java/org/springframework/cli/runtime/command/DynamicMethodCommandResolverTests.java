/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cli.runtime.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cli.runtime.engine.model.ModelPopulator;
import org.springframework.cli.runtime.engine.model.SystemModelPopulator;
import org.springframework.shell.command.CommandRegistration;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicMethodCommandResolverTests {

	@Test
	void testResolving() {
		// Create the objects that represent commands to be registered at runtime
		Map<Command, List<Command>> commands = new LinkedHashMap<>();

		// Create first command/subcommand
		Command k8sSimple = new Command("k8s-simple", "command description", null);
		Command k8sSimpleNew = new Command("new", "subcommand description", null);
		CommandOption option1 = new CommandOption.Builder()
				.withName("with-gusto")
				.withDataType("boolean")
				.withDescription("what a nice simple option")
				.withDefaultValue("true")
				.withRequired(true)
				.build();
		k8sSimpleNew.getOptions().add(option1);

		CommandOption option2 = new CommandOption.Builder()
				.withName("with-greeting")
				.withDataType("string")
				.build();
		k8sSimpleNew.getOptions().add(option2);

		// Create second command/subcommand
		Command k8sSimpleNewServices = new Command("new-services",
				"Create the Kubernetes resource files for the application's services.", null);
		CommandOption optionService1 = new CommandOption.Builder().withName("services-with-gusto").build();
		k8sSimpleNewServices.getOptions().add(optionService1);
		CommandOption optionService2 = new CommandOption.Builder().withName("services-with-greeting").build();
		k8sSimpleNewServices.getOptions().add(optionService2);

		commands.put(k8sSimple, Arrays.asList(k8sSimpleNew, k8sSimpleNewServices));

		// package up the commands in a result object
		CommandScanResults commandScanResults = new CommandScanResults(commands);

		SystemModelPopulator systemModelPopulator = new SystemModelPopulator();
		List<ModelPopulator> modelPopulators = new ArrayList<>();
		modelPopulators.add(systemModelPopulator);
		DynamicMethodCommandResolver resolver = new DynamicMethodCommandResolver(modelPopulators,
				() -> CommandRegistration.builder());
		// Move to mock scan so that we control results
		DynamicMethodCommandResolver spy = Mockito.spy(resolver);
		Mockito.when(spy.scanCommands()).thenReturn(commandScanResults);
		List<CommandRegistration> resolved = spy.resolve();

		assertThat(resolved).hasSize(2);
		assertThat(resolved).satisfiesExactly(
			registration -> {
				assertThat(registration.getCommand()).isEqualTo("k8s-simple new");
				assertThat(registration.getDescription()).isEqualTo("subcommand description");
				assertThat(registration.getOptions()).hasSize(2);
				assertThat(registration.getOptions().get(0)).satisfies(option -> {
					assertThat(option.getLongNames()).contains("with-gusto");
					assertThat(option.getType().getType()).isEqualTo(Boolean.class);
					assertThat(option.getDescription()).isEqualTo("what a nice simple option");
					assertThat(option.getDefaultValue()).isEqualTo("true");
					assertThat(option.isRequired()).isTrue();
				});
				assertThat(registration.getOptions().get(1)).satisfies(option -> {
					assertThat(option.getLongNames()).contains("with-greeting");
					assertThat(option.getType().getType()).isEqualTo(String.class);
				});
			},
			registration -> {
				assertThat(registration.getCommand()).isEqualTo("k8s-simple new-services");
			}
		);
	}
}

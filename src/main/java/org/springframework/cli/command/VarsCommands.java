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


package org.springframework.cli.command;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.context.ComponentContext;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.standard.AbstractShellComponent;

import static org.springframework.cli.util.JavaUtils.inferType;

@Command(command = "vars", group = "Vars")
public class VarsCommands extends AbstractShellComponent {

	private final ComponentFlow.Builder componentFlowBuilder;

	public VarsCommands(ComponentFlow.Builder componentFlowBuilder) {
		this.componentFlowBuilder = componentFlowBuilder;
	}

	@Command(command = "new", description = "create a new var")
	public String init() {
		String resultValue = "";
		ComponentFlow wizard = ComponentFlow.builder().reset()
				.withStringInput("language_id") //this is the variable name
				.name("language_name") // This is the text string the user sees.
				.resultValue(resultValue)
				.resultMode(ResultMode.ACCEPT)
				.and().build();
		ComponentFlowResult componentFlowResult = wizard.run();
		ComponentContext<?> resultContext = componentFlowResult.getContext();

		Object obj = resultContext.get("language_id");
		System.out.println("Collected " + obj + " as value.  Inferred type " + inferType(obj).getClass());
		return "Flow completed";
	}
}

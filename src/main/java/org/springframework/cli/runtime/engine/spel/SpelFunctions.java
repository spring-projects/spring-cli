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

package org.springframework.cli.runtime.engine.spel;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.roles.RoleService;
import org.springframework.cli.runtime.engine.actions.Exec;
import org.springframework.cli.runtime.engine.actions.handlers.ExecActionHandler;
import org.springframework.util.StringUtils;

public class SpelFunctions {

	private final ExecActionHandler execActionHandler;

	private final Path cwd;

	public SpelFunctions(ExecActionHandler execActionHandler, Path cwd) {
		this.execActionHandler = execActionHandler;
		this.cwd = cwd;
	}

	public String run(String input) {
		Exec exec = new Exec(null, input, null, null, null, null);
		Map<String, Object> outputs = new HashMap<>();
		execActionHandler.executeShellCommand(exec, outputs);
		return outputs.get(ExecActionHandler.OUTPUT_STDOUT).toString();
	}

	public String runFile(String commandFile) {
		Exec exec = new Exec(null, null, commandFile, null, null, null);
		Map<String, Object> outputs = new HashMap<>();
		execActionHandler.executeShellCommand(exec, outputs);
		return outputs.get(ExecActionHandler.OUTPUT_STDOUT).toString();
	}

	public boolean varNotDefined(String name, String... roles) {
		if (!StringUtils.hasText(name)) {
			return false;
		}
		RoleService roleService = new RoleService(cwd);
		// TODO load from default role for now, later a list of roles
		Map<String, Object> varMap;
		if (roles.length == 0) {
			varMap = roleService.loadAsMap("");
		}
		else {
			throw new SpringCliException("Roles not yet supported in SpelFunctions");
		}
		return varMap != null && !varMap.containsKey(name);
	}

}

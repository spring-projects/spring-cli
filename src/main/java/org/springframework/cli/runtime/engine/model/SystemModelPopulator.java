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

package org.springframework.cli.runtime.engine.model;

import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * A {@link ModelPopulator} that adds various {@link System} related variables and helpers
 * to the model.
 *
 * @author Eric Bottard
 */
public class SystemModelPopulator implements ModelPopulator {

	@Override
	public void contributeToModel(Path rootDirectory, Map<String, Object> model) {
		// Common model variables
		model.put("now", new Date().toString());
		ConfigurableEnvironment environment = new StandardEnvironment();
		model.put("system-properties", environment.getSystemProperties());
		model.put("system-environment", environment.getSystemEnvironment());
		model.put("tmp-dir", environment.getSystemProperties().get("java.io.tmpdir"));
		model.put("file-separator", environment.getSystemProperties().get("file.separator"));
		model.put("os-name", environment.getSystemProperties().get("os.name"));
		model.put("user-name", environment.getSystemProperties().get("user.name"));
	}

}

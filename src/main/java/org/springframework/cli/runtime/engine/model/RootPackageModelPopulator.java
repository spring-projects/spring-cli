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

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.springframework.cli.util.RootPackageFinder;

public class RootPackageModelPopulator implements ModelPopulator {

	@Override
	public void contributeToModel(Path rootDirectory, Map<String, Object> model) {
		Optional<String> rootPackage = RootPackageFinder.findRootPackage(rootDirectory.toFile());
		if (rootPackage.isPresent()) {
			String packageDir = rootPackage.get().replace('.', File.separatorChar);
			model.put("root-package", rootPackage.get());
			model.put("root-package-dir", packageDir);
		}
	}

}

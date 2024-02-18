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

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.java.tree.J.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collect declared imports and annotations of a class
 */
public class CollectAnnotationAndImportInformationRecipe extends Recipe {

	private static final Logger logger = LoggerFactory.getLogger(CollectAnnotationAndImportInformationRecipe.class);

	private final List<String> declaredImports = new ArrayList<>();

	private final List<Annotation> declaredAnnotations = new ArrayList<Annotation>();

	@Override
	public String getDisplayName() {
		return "Add or Update annotations on @SpringBootApplication class";
	}

	@Override
	public String getDescription() {
		return getDisplayName();
	}

	public List<String> getDeclaredImports() {
		return declaredImports;
	}

	public List<Annotation> getDeclaredAnnotations() {
		return declaredAnnotations;
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaIsoVisitor<ExecutionContext>() {

			@Override
			public CompilationUnit visitCompilationUnit(CompilationUnit cu, ExecutionContext executionContext) {
				List<Import> imports = cu.getImports();
				for (Import anImport : imports) {
					logger.debug("import = " + anImport);
					declaredImports.add(anImport.getTypeName());
				}
				List<ClassDeclaration> classes = cu.getClasses();
				for (ClassDeclaration aClass : classes) {
					List<Annotation> allAnnotations = aClass.getAllAnnotations();
					for (Annotation allAnnotation : allAnnotations) {
						logger.debug("annotation = " + allAnnotation.printTrimmed());
						declaredAnnotations.add(allAnnotation);
					}
				}
				return cu;
			}
		};
	}

}

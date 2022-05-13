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

import java.util.Comparator;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.CompilationUnit;

public class AddAnnotationToClassRecipe extends Recipe {

	private final String annotation;

	public AddAnnotationToClassRecipe(String annotation) {
		this.annotation = annotation;
	}

	@Override
	public String getDisplayName() {
		return "Add an annotation to the class";
	}

	@Override
	protected TreeVisitor<?, ExecutionContext> getVisitor() {
		return new AddAnnotationToClassVisitor();
	}

	private class AddAnnotationToClassVisitor extends JavaIsoVisitor<ExecutionContext> {

		private Cursor getCursorToParentScope(Cursor cursor) {
			return cursor.dropParentUntil(is -> is instanceof J.NewClass || is instanceof J.ClassDeclaration);
		}




		@Override
		public ClassDeclaration visitClassDeclaration(ClassDeclaration classDecl, ExecutionContext executionContext) {

			// NOTE: not working, probably need to add code to the JavaTemplate so that it nkows about the annotation, very
			classDecl.withTemplate(
					JavaTemplate.builder(this::getCursor, annotation )
						.javaParser(() -> JavaParser.fromJavaVersion()
								.dependsOn(new String[]{
										"package org.springframework.scheduling.annotation;" +
												"public @interface EnableScheduling {" +
												"}"
								})
								.build())
					.imports("org.springframework.scheduling.annotation.EnableScheduling")
					.build(),
					classDecl.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
			);


			return super.visitClassDeclaration(classDecl, executionContext);
		}
	}
}

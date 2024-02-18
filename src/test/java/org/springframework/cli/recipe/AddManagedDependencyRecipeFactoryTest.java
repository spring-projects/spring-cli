package org.springframework.cli.recipe;

import org.apache.maven.model.Dependency;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddManagedDependencyVisitor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * @author Fabian Krüger
 */
class AddManagedDependencyRecipeFactoryTest {

	private AddManagedDependencyRecipeFactory sut = new AddManagedDependencyRecipeFactory();

	@Test
	@DisplayName("create recipe from dependency")
	void createRecipeFromDependency() {
		Dependency dependency = new Dependency();
		dependency.setGroupId("groupId");
		dependency.setArtifactId("artifactId");
		dependency.setVersion("1.0.0");
		dependency.setScope("test");
		dependency.setType("pom");
		dependency.setClassifier("classifier");

		Recipe recipe = sut.create(dependency);

		AddManagedDependencyRecipe v = (AddManagedDependencyRecipe) recipe;
		assertThat(ReflectionTestUtils.getField(v, "groupId").toString()).isEqualTo("groupId");
		assertThat(ReflectionTestUtils.getField(v, "artifactId").toString()).isEqualTo("artifactId");
		assertThat(ReflectionTestUtils.getField(v, "version").toString()).isEqualTo("1.0.0");
		assertThat(ReflectionTestUtils.getField(v, "scope").toString()).isEqualTo("test");
		assertThat(ReflectionTestUtils.getField(v, "type").toString()).isEqualTo("pom");
		assertThat(ReflectionTestUtils.getField(v, "classifier").toString()).isEqualTo("classifier");
	}

	@Test
	@DisplayName("create recipe from dependency2")
	void createRecipeFromDependency2() {
		Dependency dependency = new Dependency();
		dependency.setGroupId("groupId");
		dependency.setArtifactId("artifactId");
		dependency.setVersion("${some.version}");
		dependency.setScope("test");
		dependency.setType("pom");
		dependency.setClassifier("classifier");

		Recipe recipe = sut.create(dependency);

		TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();
		// returns anonymous Recipe that executes AddManagedDependencyVisitor
		assertThat(visitor).isInstanceOf(AddManagedDependencyVisitor.class);
		AddManagedDependencyVisitor v = (AddManagedDependencyVisitor) visitor;
		assertThat(ReflectionTestUtils.getField(v, "groupId").toString()).isEqualTo("groupId");
		assertThat(ReflectionTestUtils.getField(v, "artifactId").toString()).isEqualTo("artifactId");
		assertThat(ReflectionTestUtils.getField(v, "version").toString()).isEqualTo("${some.version}");
		assertThat(ReflectionTestUtils.getField(v, "scope").toString()).isEqualTo("test");
		assertThat(ReflectionTestUtils.getField(v, "type").toString()).isEqualTo("pom");
		assertThat(ReflectionTestUtils.getField(v, "classifier").toString()).isEqualTo("classifier");
	}

	@Test
	@DisplayName("Create Recipe from minimal XML snippet")
	void createRecipeFromMinimalXmlSnippet() {
		@Language("xml")
		String snippet = """
				    <dependency>
				        <groupId>groupId</groupId>
				        <artifactId>artifactId</artifactId>
				    </dependency>
				""";

		Recipe recipe = sut.create(snippet);
		assertThat(ReflectionTestUtils.getField(recipe, "groupId").toString()).isEqualTo("groupId");
		assertThat(ReflectionTestUtils.getField(recipe, "artifactId").toString()).isEqualTo("artifactId");
	}

	@Test
	@DisplayName("Create Recipe from complete XML snippet")
	void createRecipeFromCompleteXmlSnippet() {
		@Language("xml")
		String snippet = """
				    <dependency>
				        <groupId>groupId</groupId>
				        <artifactId>artifactId</artifactId>
				        <version>${some.version}</version>
				        <classifier>classifier</classifier>
				        <type>pom</type>
				    </dependency>
				""";

		Recipe recipe = sut.create(snippet);
		assertThat(ReflectionTestUtils.getField(recipe, "groupId").toString()).isEqualTo("groupId");
		assertThat(ReflectionTestUtils.getField(recipe, "artifactId").toString()).isEqualTo("artifactId");
		assertThat(ReflectionTestUtils.getField(recipe, "version").toString()).isEqualTo("${some.version}");
		assertThat(ReflectionTestUtils.getField(recipe, "type").toString()).isEqualTo("pom");
		assertThat(ReflectionTestUtils.getField(recipe, "classifier").toString()).isEqualTo("classifier");
	}

	@Test
	@DisplayName("Fails with invalid Xml snippet")
	void failsWithInvalidXmlSnippet() {
		@Language("xml")
		String snippet = """
				    <dependencies>
				        <dependency>
				            <groupId>groupId</groupId>
				            <artifactId>artifactId</artifactId>
				        </dependency>
				    </dependencies>
				""";

		assertThatException().isThrownBy(() -> sut.create(snippet))
			.describedAs(
					"""
							org.springframework.cli.recipe.RecipeCreationException: Could not get text value for field 'groupId' from:
							{
							  "dependency" : {
							    "groupId" : "groupId",
							    "artifactId" : "artifactId"
							  }
							}
							""");
	}

}
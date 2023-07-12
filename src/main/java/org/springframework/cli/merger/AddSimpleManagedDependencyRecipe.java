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
import java.util.Objects;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.AddManagedDependencyVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;

/**
 * This recipe is needed since recent changes to the openrewrite recpie have strict
 * version checks and a version such as ${spring-cloud-version} can't be used.
 * This is a cut-n-paste replacement that disables the version checking, making sure only
 * that the field has a not null value.
 */
public class AddSimpleManagedDependencyRecipe extends Recipe {

	private final String groupId;

	private final String artifactId;

	private final String version;

	private final String scope;

	private final String type;

	private final String classifier;

	private final String versionPattern;

	private final Boolean releasesOnly;

	private final String onlyIfUsing;

	private final Boolean addToRootPom;

	public AddSimpleManagedDependencyRecipe(final String groupId,
			final String artifactId,
			final String version,
			final @Nullable String scope,
			final @Nullable String type,
			final @Nullable String classifier,
			final @Nullable String versionPattern,
			final @Nullable Boolean releasesOnly,
			final @Nullable String onlyIfUsing,
			final @Nullable Boolean addToRootPom) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.scope = scope;
		this.type = type;
		this.classifier = classifier;
		this.versionPattern = versionPattern;
		this.releasesOnly = releasesOnly;
		this.onlyIfUsing = onlyIfUsing;
		this.addToRootPom = addToRootPom;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getScope() {
		return scope;
	}

	public String getType() {
		return type;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getVersionPattern() {
		return versionPattern;
	}

	public Boolean getReleasesOnly() {
		return releasesOnly;
	}

	public String getOnlyIfUsing() {
		return onlyIfUsing;
	}

	public Boolean getAddToRootPom() {
		return addToRootPom;
	}

	@Override
	public String getDisplayName() {
		return "Simple Maven Managed Dependency";
	}

	@Override
	public String getDescription() {
		return "Simple Maven Managed Dependency";
	}


	protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
		List<SourceFile> rootPoms = new ArrayList<>();
		for (SourceFile source : before) {
			source.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(mavenResolutionResult -> {
				if (mavenResolutionResult.getParent() == null) {
					rootPoms.add(source);
				}
			});
		}

		return ListUtils.map(before, s -> s.getMarkers().findFirst(MavenResolutionResult.class)
				.map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
					@Override
					public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
						Xml maven = super.visitDocument(document, executionContext);

						if (!Boolean.TRUE.equals(getAddToRootPom()) || rootPoms.contains(document)) {
							Validated versionValidation = Semver.validate(getVersion(), getVersionPattern());
							if (versionValidation.isValid()) {
								String versionToUse = requireNonNull(getVersion());
								if (!Objects.equals(versionToUse, existingManagedDependencyVersion())) {
									doAfterVisit(new AddManagedDependencyVisitor(getGroupId(), getArtifactId(),
											versionToUse, getScope(), getType(), getClassifier()));
									maybeUpdateModel();
								}
							}
						}

						return maven;
					}

					@Nullable
					private String existingManagedDependencyVersion() {
						return getResolutionResult().getPom().getDependencyManagement().stream()
								.map(resolvedManagedDep -> {
									if (resolvedManagedDep.matches(getGroupId(), getArtifactId(), getType(), getClassifier())) {
										return resolvedManagedDep.getGav().getVersion();
									} else if (resolvedManagedDep.getRequestedBom() != null
											&& resolvedManagedDep.getRequestedBom().getGroupId().equals(getGroupId())
											&& resolvedManagedDep.getRequestedBom().getArtifactId().equals(getArtifactId())) {
										return resolvedManagedDep.getRequestedBom().getVersion();
									}
									return null;
								})
								.filter(Objects::nonNull)
								.findFirst().orElse(null);
					}

				}.visit(s, ctx))
				.map(SourceFile.class::cast)
				.orElse(s)
		);
	}
}

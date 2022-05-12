/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.up.initializr.model;

public class Metadata {

	private ProjectType type = new ProjectType();
	private Language language = new Language();
	private BootVersion bootVersion = new BootVersion();
	private Dependencies dependencies = new Dependencies();
	private GroupId groupId = new GroupId();
	private ArtifactId artifactId = new ArtifactId();
	private Version version = new Version();
	private Name name = new Name();
	private Description description = new Description();
	private PackageName packageName = new PackageName();
	private Packaging packaging = new Packaging();
	private JavaVersion javaVersion = new JavaVersion();

	public Metadata() {
	}

	public ProjectType getType() {
		return type;
	}

	public void setType(ProjectType type) {
		this.type = type;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public BootVersion getBootVersion() {
		return bootVersion;
	}

	public void setBootVersion(BootVersion bootVersion) {
		this.bootVersion = bootVersion;
	}

	public Dependencies getDependencies() {
		return dependencies;
	}

	public void setDependencies(Dependencies dependencies) {
		this.dependencies = dependencies;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public void setGroupId(GroupId groupId) {
		this.groupId = groupId;
	}

	public ArtifactId getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(ArtifactId artifactId) {
		this.artifactId = artifactId;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public Description getDescription() {
		return description;
	}

	public void setDescription(Description description) {
		this.description = description;
	}

	public PackageName getPackageName() {
		return packageName;
	}

	public void setPackageName(PackageName packageName) {
		this.packageName = packageName;
	}

	public Packaging getPackaging() {
		return packaging;
	}

	public void setPackaging(Packaging packaging) {
		this.packaging = packaging;
	}

	public JavaVersion getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(JavaVersion javaVersion) {
		this.javaVersion = javaVersion;
	}
}

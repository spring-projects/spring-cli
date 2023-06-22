/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cli.runtime.engine.actions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;

/**
 * The actions that can be executed
 *
 * @author Mark Pollack
 */
public class Action {


	@Nullable
	private String ifExpression;

	/**
	 * Where to generate the template, relative to the {@code destinationDirectory}. May
	 * contain template placeholders. If this is null, the file is not rendered.
	 */
	@Nullable
	private Generate generate;

	@Nullable
	private final Inject inject;

	@Nullable
	private Exec exec;

	@Nullable
	private Vars vars;

	@Nullable
	private InjectMavenDependency injectMavenDependency;

	@Nullable
	private InjectMavenDependencyManagement injectMavenDependencyManagement;

	@Nullable
	private InjectMavenRepository injectMavenRepository;

	@Nullable
	private InjectMavenBuildPlugin injectMavenBuildPlugin;

	@Nullable
	private final InjectProperties injectProperties;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	Action(@JsonProperty("if") @Nullable String ifExpression,
			@JsonProperty("generate") @Nullable Generate generate,
			@JsonProperty("exec") @Nullable Exec exec,
			@JsonProperty("vars") @Nullable Vars vars,
			@JsonProperty("inject-maven-dependency") @Nullable InjectMavenDependency injectMavenDependency,
			@JsonProperty("inject-maven-dependency-management") @Nullable InjectMavenDependencyManagement injectMavenDependencyManagement,
			@JsonProperty("inject-maven-repository") @Nullable InjectMavenRepository injectMavenRepository,
			@JsonProperty("inject-maven-build-plugin") @Nullable InjectMavenBuildPlugin injectMavenBuildPlugin,
			@JsonProperty("inject-properties") @Nullable InjectProperties injectProperties,
			@JsonProperty("inject") @Nullable Inject inject) {
		this.ifExpression = ifExpression;
		this.generate = generate;
		this.exec = exec;
		this.vars = vars;
		this.injectMavenDependency = injectMavenDependency;
		this.injectMavenDependencyManagement = injectMavenDependencyManagement;
		this.injectMavenRepository = injectMavenRepository;
		this.injectMavenBuildPlugin = injectMavenBuildPlugin;
		this.injectProperties = injectProperties;
		this.inject = inject;
	}

	@Nullable
	public String getIfExpression() {
		return ifExpression;
	}

	@Nullable
	public Generate getGenerate() {
		return generate;
	}

	@Nullable
	public Exec getExec() {
		return exec;
	}

	@Nullable
	public Vars getVars() {
		return vars;
	}

	@Nullable
	public InjectMavenBuildPlugin getInjectMavenPlugin() {
		return injectMavenBuildPlugin;
	}

	@Nullable
	public InjectMavenDependency getInjectMavenDependency() {
		return injectMavenDependency;
	}

	@Nullable
	public InjectMavenBuildPlugin getInjectMavenBuildPlugin() {
		return injectMavenBuildPlugin;
	}

	@Nullable
	public InjectMavenDependencyManagement getInjectMavenDependencyManagement() {
		return injectMavenDependencyManagement;
	}

	@Nullable
	public InjectMavenRepository getInjectMavenRepository() {
		return injectMavenRepository;
	}


	@Nullable
	public InjectProperties getInjectProperties() {
		return injectProperties;
	}

	@Nullable
	public Inject getInject() {
		return inject;
	}

	@Override
	public String toString() {
		return "Action{" +
				"if='" + ifExpression + '\'' +
				", generate=" + generate +
				", inject=" + inject +
				", exec=" + exec +
				", vars=" + vars +
				", injectMavenDependency=" + injectMavenDependency +
				", injectMavenDependencyManagement=" + injectMavenDependencyManagement +
				", injectMavenRepository=" + injectMavenRepository +
				", injectMavenBuildPlugin=" + injectMavenBuildPlugin +
				", injectProperties=" + injectProperties +
				'}';
	}
}

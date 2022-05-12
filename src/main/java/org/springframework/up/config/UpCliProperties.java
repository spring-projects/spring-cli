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
package org.springframework.up.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for cli.
 *
 * @author Janne Valkealahti
 */
@ConfigurationProperties(prefix = "spring.up")
public class UpCliProperties {

	private Initializr initializr = new Initializr();

	private Github github = new Github();

	private Defaults defaults = new Defaults();

	public Initializr getInitializr() {
		return initializr;
	}

	public void setInitializr(Initializr initializr) {
		this.initializr = initializr;
	}

	public Github getGithub() {
		return github;
	}

	public void setGithub(Github github) {
		this.github = github;
	}

	public Defaults getDefaults() {
		return defaults;
	}

	public void setDefaults(Defaults defaults) {
		this.defaults = defaults;
	}

	/**
	 * Settings for spring initializr.
	 */
	public static class Initializr {
		private String baseUrl = "https://start.spring.io";

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		@Override
		public String toString() {
			return "Initializr{" +
					"baseUrl='" + baseUrl + '\'' +
					'}';
		}
	}

	public static class Github {

		/**
		 * OAuth client id for github oauth app which user is seeing when doing auth
		 * flow. This is public so safe to expose.
		 */
		private String clientId;

		/**
		 * Default scopes auth flow requests from a user.
		 */
		private String defaultScopes = "repo,read:org";

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getDefaultScopes() {
			return defaultScopes;
		}

		public void setDefaultScopes(String defaultScopes) {
			this.defaultScopes = defaultScopes;
		}

		@Override
		public String toString() {
			return "Github{" +
					"clientId='" + clientId + '\'' +
					", defaultScopes='" + defaultScopes + '\'' +
					'}';
		}
	}

	@Override
	public String toString() {
		return "UpCliProperties{" +
				"initializr=" + initializr +
				", github=" + github +
				", defaults=" + defaults +
				'}';
	}
}

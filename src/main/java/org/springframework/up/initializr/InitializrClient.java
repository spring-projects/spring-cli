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
package org.springframework.up.initializr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.up.initializr.model.Metadata;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Interface and implementation talking with initializr.
 *
 * @author Janne Valkealahti
 */
public interface InitializrClient {

	/**
	 * Connect with a system.
	 */
	void connect();

	/**
	 * Get initializr metadata.
	 *
	 * @return the metadata
	 */
	Metadata getMetadata();

	/**
	 * Get info about a system.

	 * @return info about a system
	 */
	String info();

	/**
	 * Generate a project as a tgz file and return a path to it.
	 *
	 * @return the path to generated tgz project file
	 */
	Path generate(String projectType, String languageType, String bootVersion, List<String> dependencies,
			String version, String groupId, String artifact, String name, String description, String packageName,
			String packaging, String javaVersion);

	/**
	 * Interface for a initializr client builder.
	 */
	interface Builder {

		/**
		 * Sets a target system, i.e. 'https://start.spring.io'.
		 *
		 * @param baseUrl the base url
		 * @return the builder
		 */
		Builder target(String baseUrl);

		/**
		 * Builds an initializr client.
		 *
		 * @return the initializr client
		 */
		InitializrClient build();
	}

	/**
	 * Gets a new builder instance for initializr client.
	 *
	 * @param webClientBuilder the webclient builder
	 * @return the builder for initializr client
	 */
	public static Builder builder(WebClient.Builder webClientBuilder) {
		return new DefaultBuilder(webClientBuilder);
	}

	public static class DefaultBuilder implements Builder {

		private String baseUrl;
		private WebClient.Builder webClientBuilder;

		DefaultBuilder(WebClient.Builder webClientBuilder) {
			this.webClientBuilder = webClientBuilder;
		}

		public Builder target(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public InitializrClient build() {
			WebClient client = webClientBuilder
					.baseUrl(this.baseUrl)
					.build();
			return new DefaultInitializrClient(client, this.baseUrl);
		}
	}

	public static class DefaultInitializrClient implements InitializrClient {

		private final static MediaType INITIALIZER_MEDIA_TYPE = new MediaType("application", "vnd.initializr.v2.2+json");
		private WebClient client;
		private Metadata metadata;
		private AtomicBoolean connected = new AtomicBoolean(false);
		private String baseUrl;

		public DefaultInitializrClient(WebClient client, String baseUrl) {
			this.client = client;
			this.baseUrl = baseUrl;
		}

		@Override
		public void connect() {
			if (connected.compareAndSet(false, true)) {
				updateMetadata();
			}
		}

		@Override
		public Metadata getMetadata() {
			if (!connected.get()) {
				connect();
			}
			return metadata;
		}

		@Override
		public String info() {
			return baseUrl;
		}

		@Override
		public Path generate(String projectType, String languageType, String bootVersion, List<String> dependencies,
				String version, String groupId, String artifact, String name, String description, String packageName, String packaging,
				String javaVersion) {
			try {
				Path tmp = Files.createTempFile("initializrcli", null);
				Flux<DataBuffer> dataBuffer = client.get()
						.uri(uriBuilder -> uriBuilder.path("starter.tgz")
								.queryParam("type", projectType)
								.queryParam("dependencies", StringUtils.collectionToCommaDelimitedString(dependencies))
								.queryParam("packaging", packaging)
								.queryParam("javaVersion", javaVersion)
								.queryParam("language", languageType)
								.queryParam("bootVersion", bootVersion)
								.queryParam("version", version)
								.queryParam("groupId", groupId)
								.queryParam("artifactId", artifact)
								.queryParam("name", name)
								.queryParam("description", description)
								.queryParam("packageName", packageName)
								.build())
						.accept(MediaType.ALL)
						.retrieve().bodyToFlux(DataBuffer.class);
				DataBufferUtils.write(dataBuffer, tmp).block();
				return tmp;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void updateMetadata() {
			this.metadata = client.get()
				.accept(INITIALIZER_MEDIA_TYPE)
				.retrieve()
				.toEntity(Metadata.class).block().getBody();
		}
	}
}

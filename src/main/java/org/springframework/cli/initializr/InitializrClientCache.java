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
package org.springframework.cli.initializr;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Simple cache/factory implementation for {@link InitializrClient}.
 *
 * @author Janne Valkealahti
 */
public class InitializrClientCache {

	private final WebClient.Builder webClientBuilder;
	private final Map<String, InitializrClient> cache = new HashMap<>();

	public InitializrClientCache(WebClient.Builder webClientBuilder) {
		Assert.notNull(webClientBuilder, "webClientBuilder must be set");
		this.webClientBuilder = webClientBuilder;
	}

	/**
	 * Get {@link InitializrClient} with a given url and cache it for next use.
	 *
	 * @param url the initializr url
	 * @return initializr client
	 */
	public InitializrClient get(String url) {
		return cache.computeIfAbsent(url, baseUrl -> {
			return InitializrClient.builder(webClientBuilder)
				.target(baseUrl)
				.build();
		});
	}
}

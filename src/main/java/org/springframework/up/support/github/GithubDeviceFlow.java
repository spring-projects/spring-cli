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
package org.springframework.up.support.github;

import java.time.Duration;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.retry.Backoff;
import reactor.util.retry.Retry;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utility methods for github auth flows.
 *
 * @author Janne Valkealahti
 */
public abstract class GithubDeviceFlow {

	private final static ParameterizedTypeReference<Map<String, String>> RESPONSE_TYPE_REFERENCE =
			new ParameterizedTypeReference<Map<String, String>>() {};

	/**
	 * Starts a device flow. Makes a simple request to github api to request new
	 * device code which user can use to complete authentication process.
	 *
	 * @param webClientBuilder the web client builder
	 * @param clientId the client id
	 * @param scope the scopes
	 * @return Map of response values
	 */
	public static Map<String, String> requestDeviceFlow(WebClient.Builder webClientBuilder, String clientId, String scope) {
		WebClient client = webClientBuilder
			.baseUrl("https://github.com")
			.build();
		Mono<Map<String, String>> response = client.post()
			.uri(uriBuilder -> uriBuilder
					.path("login/device/code")
					.queryParam("client_id", clientId)
					.queryParam("scope", scope)
					.build())
			.accept(MediaType.APPLICATION_JSON)
			.retrieve().bodyToMono(RESPONSE_TYPE_REFERENCE);
		Map<String, String> values = response.block();
		return values;
	}

	/**
	 * Waits user to enter code based on info api gave us from initial device flow request.
	 *
	 * @param clientId the client id
	 * @param deviceCode the device code
	 * @param timeout the timeout
	 * @param interval the interval
	 * @return a token
	 */
	public static String waitTokenFromDeviceFlow(WebClient.Builder webClientBuilder, String clientId, String deviceCode,
			int timeout, int interval) {
		WebClient client = webClientBuilder
			.baseUrl("https://github.com")
			.build();
		Mono<String> accessToken = client.post()
			.uri(uriBuilder -> uriBuilder
					.path("login/oauth/access_token")
					.queryParam("client_id", clientId)
					.queryParam("device_code", deviceCode)
					.queryParam("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
					.build())
			.accept(MediaType.APPLICATION_JSON)
			.exchangeToMono(response -> {
				return response.bodyToMono(RESPONSE_TYPE_REFERENCE);
			})
			.flatMap(data -> {
				String token = data.get("access_token");
				if (StringUtils.hasText(token)) {
					return Mono.just(token);
				}
				else {
					return Mono.error(new NoAccessTokenException());
				}
			})
			.retryWhen(Retry.withThrowable(reactor.retry.Retry.onlyIf(x -> {
					if (x.exception() instanceof NoAccessTokenException) {
						return true;
					}
					else {
						return false;
					}
				})
				.timeout(Duration.ofSeconds(timeout))
				.backoff(Backoff.fixed(Duration.ofSeconds(interval)))
			));
			return accessToken.block();
	}

	/**
	 * Used in a reactor chain to retry when poll/retry request don't
	 * yet have a token.
	 */
	private static class NoAccessTokenException extends RuntimeException {
	}
}

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
package org.springframework.cli.support.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class GithubDeviceFlowTests {

	private MockWebServer server;

	private WebClient.Builder webClientBuilder;

	private String baseUri;

	@BeforeEach
	void setup() throws IOException {
		this.server = new MockWebServer();
		this.server.start();
		this.webClientBuilder = WebClient.builder();
		this.baseUri = "http://" + this.server.getHostName() + ":" + this.server.getPort();
	}

	@AfterEach
	void cleanup() throws Exception {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Test
	void testRequestDeviceFlow() throws IOException {
		MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.OK.value())
			.setBody(new ObjectMapper().writeValueAsString(getRequestDeviceFlowResponse()))
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponse);

		GithubDeviceFlow githubDeviceFlow = new GithubDeviceFlow(baseUri);
		Map<String, String> response = githubDeviceFlow.requestDeviceFlow(webClientBuilder, "fakeClientId",
				"fakeScope");
		assertThat(response).containsKey("device_code");
	}

	@Test
	void testWaitTokenFromDeviceFlowSucceed() throws IOException {
		for (int i = 0; i < 3; i++) {
			MockResponse mockResponseWithoutToken = new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setBody(new ObjectMapper().writeValueAsString(getAccessTokenResponse(false)))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			this.server.enqueue(mockResponseWithoutToken);
		}
		MockResponse mockResponseWithToken = new MockResponse().setResponseCode(HttpStatus.OK.value())
			.setBody(new ObjectMapper().writeValueAsString(getAccessTokenResponse(true)))
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponseWithToken);

		GithubDeviceFlow githubDeviceFlow = new GithubDeviceFlow(baseUri);
		Optional<String> waitTokenFromDeviceFlow = githubDeviceFlow.waitTokenFromDeviceFlow(webClientBuilder,
				"clientId", "deviceCode", 6, 1);
		assertThat(waitTokenFromDeviceFlow).hasValue("gho_16C7e42F292c6912E7710c838347Ae178B4a");
	}

	@Test
	void testWaitTokenFromDeviceFlowFails() throws IOException {
		for (int i = 0; i < 3; i++) {
			MockResponse mockResponseWithoutToken = new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setBody(new ObjectMapper().writeValueAsString(getAccessTokenResponse(false)))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			this.server.enqueue(mockResponseWithoutToken);
		}
		MockResponse mockResponseWithToken = new MockResponse().setResponseCode(HttpStatus.OK.value())
			.setBody(new ObjectMapper().writeValueAsString(getAccessTokenResponse(true)))
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponseWithToken);

		GithubDeviceFlow githubDeviceFlow = new GithubDeviceFlow(baseUri);
		Optional<String> waitTokenFromDeviceFlow = githubDeviceFlow.waitTokenFromDeviceFlow(webClientBuilder,
				"clientId", "deviceCode", 2, 1);
		assertThat(waitTokenFromDeviceFlow).isEmpty();
	}

	private Map<String, Object> getRequestDeviceFlowResponse() {
		Map<String, Object> response = new HashMap<>();
		response.put("device_code", "3584d83530557fdd1f46af8289938c8ef79f9dc5");
		response.put("user_code", "WDJB-MJHT");
		response.put("verification_uri", "https://github.com/login/device");
		response.put("expires_in", 900);
		response.put("interval", 5);
		return response;
	}

	private Map<String, Object> getAccessTokenResponse(boolean token) {
		Map<String, Object> response = new HashMap<>();
		if (token) {
			response.put("access_token", "gho_16C7e42F292c6912E7710c838347Ae178B4a");
			response.put("token_type", "bearer");
			response.put("scope", "repo,gist");
		}
		return response;
	}

}

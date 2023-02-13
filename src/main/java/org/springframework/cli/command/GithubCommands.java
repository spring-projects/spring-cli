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
package org.springframework.cli.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jline.utils.AttributedString;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.config.SpringCliUserConfig.Host;
import org.springframework.cli.config.SpringCliUserConfig.Hosts;
import org.springframework.cli.support.github.GithubDeviceFlow;
import org.springframework.cli.util.SpringCliTerminal;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.style.StyleSettings;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

/**
 * Commands for github authentication.
 *
 * @author Janne Valkealahti
 */
@ShellComponent
public class GithubCommands extends AbstractSpringCliCommands {

	private final static Logger log = LoggerFactory.getLogger(GithubCommands.class);

	private final static RateLimitChecker RATE_LIMIT_CHECKER = new RateLimitChecker.LiteralValue(0);

	private WebClient.Builder webClientBuilder;

	private ComponentFlow.Builder componentFlowBuilder;

	private SpringCliUserConfig userConfig;

	private SpringCliTerminal terminal;

	@Autowired
	public GithubCommands(Builder webClientBuilder, ComponentFlow.Builder componentFlowBuilder, SpringCliUserConfig userConfig, SpringCliTerminal springCliTerminal) {
		this.webClientBuilder = webClientBuilder;
		this.componentFlowBuilder = componentFlowBuilder;
		this.userConfig = userConfig;
		this.terminal = springCliTerminal;
	}

	/**
	 * Login command for github. Makes user to choose either starting a device flow
	 * via browser or pasting a token created manually.
	 */
	@ShellMethod(key = "github auth login", value = "Authenticate with a GitHub")
	public void login() {
		String authType = askAuthType();
		String clientId = getCliProperties().getGithub().getClientId();
		String scopes = getCliProperties().getGithub().getDefaultScopes();

		if (ObjectUtils.nullSafeEquals(authType, "web")) {
			GithubDeviceFlow githubDeviceFlow = new GithubDeviceFlow("https://github.com");
			Map<String, String> response = githubDeviceFlow.requestDeviceFlow(webClientBuilder, clientId, scopes);

			AttributedString styledStr = terminal.styledString("!", StyleSettings.TAG_LEVEL_WARN);
			styledStr = terminal.join(styledStr,
					terminal.styledString(" Open browser with https://github.com/login/device and paste device code ", null));
			styledStr = terminal.join(styledStr, terminal.styledString(response.get("user_code"), StyleSettings.TAG_HIGHLIGHT));
			terminal.print(styledStr);

			Optional<String> token = githubDeviceFlow.waitTokenFromDeviceFlow(webClientBuilder, clientId,
					response.get("device_code"),
					Integer.parseInt(response.get("expires_in")),
					Integer.parseInt(response.get("interval")));
			if (token.isPresent()) {
				userConfig.updateHost("github.com", new Host(token.get(), null));
				terminal.print("logged in to github");
			}
			else {
				terminal.print("failed logging in to github");
			}
		}
		else if (ObjectUtils.nullSafeEquals(authType, "paste")) {
			terminal.print("Tip: you can generate a Personal Access Token here https://github.com/settings/tokens");
			terminal.print("The minimum required scopes are 'repo', 'read:org'");
			String token = askToken();
			userConfig.updateHost("github.com", new Host(token, null));
			terminal.print("logged in to github");
		}
	}

	/**
	 * Logout command which essentially removes local token if exists.
	 */
	@ShellMethod(key = "github auth logout", value = "Log out of a GitHub")
	public void logout() {
		Host host = null;
		Map<String, Host> hostsMap = userConfig.getHosts();
		if (hostsMap == null) {
			hostsMap = new HashMap<>();
		}
		else {
			host = hostsMap.get("github.com");
		}
		if (host == null) {
			terminal.print("not logged in to github");
		}
		else {
			hostsMap.remove("github.com");
			Hosts hosts = new Hosts();
			hosts.setHosts(hostsMap);
			userConfig.setHosts(hosts);
			terminal.print("removed authentication token");
		}
	}

	/**
	 * Shows current authentication status for a github.
	 *
	 * @param showToken flag if actual token should be shown.
	 * @return the content
	 */
	@ShellMethod(key = "github auth status", value = "View authentication status")
	public AttributedString status(
			@ShellOption(help = "Display the auth token", defaultValue = "false") boolean showToken
		) {
		Host host = null;
		Map<String, Host> hosts = userConfig.getHosts();
		if (hosts != null) {
			host = hosts.get("github.com");
		}
		if (host == null) {
			return new AttributedString("You are not logged into github");
		}
		else {
			String loginName = null;
			try {
				GitHub gh = new GitHubBuilder()
						.withOAuthToken(host.getOauthToken())
						.withRateLimitChecker(RATE_LIMIT_CHECKER)
						.build();
				loginName = gh.getMyself().getLogin();
				log.debug("Got loginName {}", loginName);
			} catch (IOException e) {
				log.error("Error getting github login", e);
			}
			AttributedString ret = terminal.styledString("You are logged into github as ", null);
			ret = terminal.join(ret, terminal.styledString(loginName, StyleSettings.TAG_HIGHLIGHT));
			if (showToken) {
				ret = terminal.join(ret, terminal.styledString(", with token ", null));
				ret = terminal.join(ret, terminal.styledString(host.getOauthToken(), StyleSettings.TAG_HIGHLIGHT));
			}
			return ret;
		}
	}

	/**
	 * Asks if user wants to auth by giving a token or going through web flow.
	 *
	 * @return either web or paste
	 */
	private String askAuthType() {
		Map<String, String> authType = new HashMap<>();
		authType.put("Login with a web browser", "web");
		authType.put("Paste an authentication token", "paste");
		ComponentFlow wizard = componentFlowBuilder.clone().reset()
				.withSingleItemSelector("authType")
					.name("How would you like to authenticate GitHub CLI")
					.resultMode(ResultMode.ACCEPT)
					.selectItems(authType)
					.and()
				.build();
		ComponentFlowResult run = wizard.run();
		return run.getContext().get("authType");
	}

	/**
	 * Asks user to paste a token.
	 *
	 * @return token user gave
	 */
	private String askToken() {
		ComponentFlow wizard = componentFlowBuilder.clone().reset()
				.resourceLoader(getResourceLoader())
				.templateExecutor(getTemplateExecutor())
				.withStringInput("token")
					.name("Paste your authentication token:")
					.maskCharacter('*')
					.and()
				.build();
		ComponentFlowResult run = wizard.run();
		return run.getContext().get("token");
	}
}

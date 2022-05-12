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

package org.springframework.up.git;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * @author Thomas Risberg
 */
public final class GitRepoUrlRef {

	private static final Logger logger = LoggerFactory.getLogger(GitRepoUrlRef.class);

	private final URL repoUrl;

	private final String subPath;

	private final String ref;

	public GitRepoUrlRef(URL repoUrl, String subPath, String ref) {
		this.repoUrl = repoUrl;
		this.subPath = subPath;
		this.ref = ref;
	}

	//@formatter:off
	/**
	 * @param uriString The https:// URI for a Git repo. Supported variants are:
	 *  1. the URL used for cloning with HTTPS with optional 'ref' and 'subPath' query parameters; and
	 *  2. the URL copied from the browser address bar when browsing the repo for a specific tag or branch.
	 * @return A GitRepoUrlRef populated based on the values parsed from the uriString
	 * provided
	 */
	//@formatter:on
	public static GitRepoUrlRef fromUriString(@NonNull String uriString) {
		Assert.notNull(uriString, "The uriString must not be null");

		URI uri = null;

		try {
			uri = new URI(uriString);
			if (uri.getPath().contains("/tree/") || uri.getPath().contains("/tags/")) {
				uri = extractSubPathAndRef(uri);
			}
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URI provided: '" + uriString + "'", e);
		}
		return fromParsedUri(uri);
	}

	private static GitRepoUrlRef fromParsedUri(@NonNull URI uri) {
		Assert.notNull(uri, "The uri must not be null");

		if (uri.getScheme() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
			throw new IllegalArgumentException("The URI scheme must be 'https' or 'http'");
		}
		if (uri.getHost() == null) {
			throw new IllegalArgumentException("The URI host must not be null");
		}

		URL repoUrl = null;
		try {
			repoUrl = new URL(uri.getScheme() + "://" + uri.getHost() + uri.getPath());
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("Unable to construct URL for Git repository", e);
		}
		String ref = null;
		String subPath = null;

		String[] parameters = {};
		if (uri.getQuery() != null) {
			parameters = uri.getQuery().split("&");
		}
		for (String p : parameters) {
			if (p.startsWith("ref=")) {
				ref = p.substring(4);
			}
			else if (p.startsWith("subPath=")) {
				subPath = p.substring(8);
			}
			else {
				throw new IllegalArgumentException(
						"Query parameter " + p + " is invalid, only 'ref' and 'subPath' are supported");
			}
		}
		return new GitRepoUrlRef(repoUrl, subPath, ref);

	}

	private static URI extractSubPathAndRef(URI uri) throws URISyntaxException {
		String webUrlPath = uri.getPath();
		int start = 0;
		int len = 0;
		if (webUrlPath.contains("/-/tags/")) {
			start = webUrlPath.indexOf("/-/tags/");
			len = 8;
		}
		else if (webUrlPath.contains("/-/tree/")) {
			start = webUrlPath.indexOf("/-/tree/");
			len = 8;
		}
		else if (webUrlPath.contains("/tree/")) {
			start = webUrlPath.lastIndexOf("/tree/");
			len = 6;
		}
		else {
			throw new IllegalArgumentException("Invalid path '" + webUrlPath + "'");
		}
		String path = webUrlPath.substring(0, start) + ".git";
		String tree = webUrlPath.substring(start + len);
		String ref = null;
		String subPath = null;
		if (tree.contains("/")) {
			if (tree.length() > 1) {
				subPath = tree.substring(tree.indexOf("/") + 1);
			}
			ref = tree.substring(0, tree.indexOf("/"));
		}
		else {
			ref = tree;
		}
		String query = null;
		if (ref.length() > 0) {
			query = "ref=" + ref;
		}
		if (subPath != null) {
			if (query == null) {
				query = "subPath=" + subPath;
			}
			else {
				query += "&subPath=" + subPath;
			}
		}
		return new URI(uri.getScheme(), uri.getHost(), path, query, null);
	}

	public URL getRepoUrl() {
		return repoUrl;
	}

	public String getSubPath() {
		return subPath;
	}

	public String getRef() {
		return ref;
	}

	@Override
	public String toString() {
		return "GitRepoUrlRef{" + "repoUrl=" + repoUrl + ", subPath='" + subPath + '\'' + ", ref='" + ref + '\'' + '}';
	}

}

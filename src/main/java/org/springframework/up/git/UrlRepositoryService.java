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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.gitlab4j.api.Constants.ArchiveFormat;
import org.gitlab4j.api.Constants.SortOrder;
import org.gitlab4j.api.Constants.TagOrderBy;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Tag;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.up.UpException;
import org.springframework.up.config.TemplateRepositoryProperties;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * @author Thomas Risberg
 */
@Component
public class UrlRepositoryService implements SourceRepositoryService {

	private final Logger logger = LoggerFactory.getLogger(UrlRepositoryService.class);

	private final TemplateRepositoryProperties templateRepositoryProperties;

	public UrlRepositoryService(TemplateRepositoryProperties templateRepositoryProperties) {
		this.templateRepositoryProperties = templateRepositoryProperties;
	}

	@Override
	public Path retrieveRepositoryContents(String sourceRepoUrl) {
		Path targetPath;
		try {
			targetPath = Files.createTempDirectory("source-repo-");
		}
		catch (IOException e) {
			throw new UpException("Failed to create temp directory: " + e.getMessage(), e);
		}

		Path contentPath;
		if (sourceRepoUrl.startsWith("file:")) {
			contentPath = retrieveFileContents(sourceRepoUrl, targetPath);
		}
		else {
			GitRepoUrlRef gitRepoUrlRef = GitRepoUrlRef.fromUriString(sourceRepoUrl);
			if (gitRepoUrlRef.getRepoUrl().toString().contains("github.com")) {
				contentPath = retrieveGitHubRepositoryContents(gitRepoUrlRef, targetPath);
			}
			else {
				contentPath = retrieveGitLabRepositoryContents(gitRepoUrlRef, targetPath);
			}
		}
		logger.debug("Source from " + sourceRepoUrl + " retrieved into " + contentPath.toFile().getAbsolutePath());
		return contentPath;
	}

	/**
	 * Retrieve contents from a file location.
	 */
	private Path retrieveFileContents(String source, Path targetPath) {
		try {
			File src = ResourceUtils.getFile(source);
			File dest = targetPath.toFile();
			logger.debug("Copying file resource: " + src + " to " + dest);
			FileSystemUtils.copyRecursively(src, dest);
		}
		catch (IOException e) {
			throw new UpException("Failed processing " + source, e);
		}
		return targetPath;
	}

	/**
	 * Retrieve contents from a GitHub repository.
	 */
	private Path retrieveGitHubRepositoryContents(GitRepoUrlRef url, Path targetPath) {

		try {
			URI gitUri = new URI(url.getRepoUrl().toString());
			String token = this.templateRepositoryProperties.getTokens().get(gitUri.getHost());
			GitHub github;
			if (token == null) {
				github = GitHub.connectAnonymously();
			}
			else {
				github = new GitHubBuilder().withOAuthToken(token).build();
			}
			String repo = gitUri.getPath().substring(1);
			if (repo.endsWith(".git")) {
				repo = repo.substring(0, repo.length() - 4);
			}
			String ref = url.getRef();
			GHRepository ghRepository = github.getRepository(repo);
			InputStream inputStream = ghRepository
					.readTar((inputstream) -> new ByteArrayInputStream(StreamUtils.copyToByteArray(inputstream)), ref);

			File targetFile = targetPath.toFile();
			Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
			try {
				archiver.extract(inputStream, targetPath.toFile());
			} catch (Exception e) {
				throw new UpException(String.format("Extraction error to %s", targetFile.getAbsolutePath()), e);
			}

			Path unTar = Paths.get(targetPath.toFile().getAbsolutePath());
			AtomicReference<Path> tarDir = new AtomicReference<>();
			Files.list(unTar.toFile().toPath()).forEach((path) -> {
				if (path.toFile().isDirectory()) {
					if (tarDir.get() != null) {
						throw new UpException("Detected multiple directories '" + tarDir.get().toFile().getName()
								+ "' and '" + path.toFile().getName() + " in downloaded zip file");
					}
					tarDir.set(path);
				}
			});
			if (tarDir.get() == null) {
				throw new UpException(
						"Downloaded zip file not unzipped correctly into " + unTar.toFile().getAbsolutePath());
			}
			Path contentPath;
			if (StringUtils.hasText(url.getSubPath())) {
				contentPath = Paths.get(tarDir.get().toFile().getAbsolutePath(), url.getSubPath());
			}
			else {
				contentPath = tarDir.get();
			}
			return contentPath;
		}
		catch (IOException | URISyntaxException e) {
			throw new UpException("Failed processing " + url, e);
		}
	}

	/**
	 * Retrieve contents from a GitLab repository.
	 */
	private Path retrieveGitLabRepositoryContents(GitRepoUrlRef url, Path targetPath) {
		try {
			URI gitUri = new URI(url.getRepoUrl().toString());
			String token = this.templateRepositoryProperties.getTokens().get(gitUri.getHost());
			if (token == null) {
				throw new UpException("Access token not provided for " + gitUri);
			}
			GitLabApi gitLabApi = new GitLabApi(gitUri.getScheme() + "://" + gitUri.getHost(), token);
			String repo = gitUri.getPath().substring(1);
			if (repo.endsWith(".git")) {
				repo = repo.substring(0, repo.length() - 4);
			}

			String refSha = null;
			if (StringUtils.hasText(url.getRef())) {
				List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(repo, url.getRef());
				if (branches.size() == 1) {
					refSha = branches.get(0).getCommit().getId();
				}
				else {
					List<Tag> tags = gitLabApi.getTagsApi().getTags(repo, TagOrderBy.NAME, SortOrder.ASC, url.getRef());
					if (tags.size() == 1) {
						refSha = tags.get(0).getCommit().getId();
					}
					else {
						throw new UpException("Not able to find ref " + url.getRef() + " for " + repo);
					}
				}
			}
			File tarfile = gitLabApi.getRepositoryApi().getRepositoryArchive(repo, refSha, targetPath.toFile(),
					ArchiveFormat.TAR_GZ);
			logger.debug("Wrote GitLab Repo " + repo + " to " + tarfile.getAbsolutePath());

			File targetFile = targetPath.toFile();
			Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
			try {
				archiver.extract(tarfile, targetPath.toFile());
			} catch (Exception e) {
				throw new UpException(String.format("Extraction error to %s", targetFile.getAbsolutePath()), e);
			}

			String zipDirName = tarfile.getName().substring(0, tarfile.getName().indexOf('.'));
			if (!tarfile.delete()) {
				logger.warn("Not able to delete zip file " + tarfile.getAbsolutePath());
			}
			Path contentPath;
			if (StringUtils.hasText(url.getSubPath())) {
				contentPath = Paths.get(targetPath.toFile().getAbsolutePath(), zipDirName, url.getSubPath());
			}
			else {
				contentPath = Paths.get(targetPath.toFile().getAbsolutePath(), zipDirName);
			}
			return contentPath;
		}
		catch (URISyntaxException | GitLabApiException e) {
			throw new UpException("Failed processing " + url, e);
		}
	}

}

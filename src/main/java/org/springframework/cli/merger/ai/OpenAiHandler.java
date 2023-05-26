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


package org.springframework.cli.merger.ai;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.templating.HandlebarsTemplateEngine;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.PropertyFileUtils;
import org.springframework.cli.util.RootPackageFinder;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OpenAiHandler implements AiHandler {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiHandler.class);

	private final HandlebarsTemplateEngine handlebarsTemplateEngine = new HandlebarsTemplateEngine();

	public void add(String description, String path, boolean preview, TerminalMessage terminalMessage) {
		Path projectPath = getProjectPath(path);

		ProjectNameHeuristic projectNameHeuristic = new ProjectNameHeuristic();
		logger.debug("Deriving main Spring project required...");
		ProjectName projectName = projectNameHeuristic.deriveProjectName(description);
		logger.debug("Done.  The code will primarily use " + projectName.getSpringProjectName());

		Map<String, String> context = getContext(description, projectName, projectPath);
		PromptRequest promptRequest = createPrompt(context);
		String completionEstimate = getCompletionEstimate();
		terminalMessage.print("Generating code.  This will take a few minutes ..." +
				" Check back around " + completionEstimate);
		String response = generate(promptRequest);
		terminalMessage.print("Done.");

		ResponseModifier responseModifier = new ResponseModifier();
		String modifiedResponse = responseModifier.modify(response, description);
		writeReadMe(projectName, modifiedResponse, projectPath, terminalMessage);
		if (!preview) {
			List<ProjectArtifact> projectArtifacts = createProjectArtifacts(modifiedResponse);
			ProjectArtifactProcessor projectArtifactProcessor = new ProjectArtifactProcessor(projectArtifacts,  projectPath, terminalMessage);
			ProcessArtifactResult processArtifactResult = projectArtifactProcessor.process();
		}
	}

	private String getCompletionEstimate() {
		TimeZone userTimeZone = TimeZone.getDefault();
		LocalTime estimatedTime = LocalTime.now().plusMinutes(3);
		DateTimeFormatter formatter;
		if (userTimeZone.inDaylightTime(new java.util.Date())) {
			// If the user's time zone is in daylight saving time, use 12-hour format with AM/PM
			formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
		} else {
			// Otherwise, use 24-hour format
			formatter = DateTimeFormatter.ofPattern("HH:mm");
		}
		return estimatedTime.format(formatter);
	}

	public void apply(String file, String path, TerminalMessage terminalMessage) {
		Path projectPath = getProjectPath(path);
		Path readmePath = projectPath.resolve(file);
		if (Files.notExists(readmePath)) {
			throw new SpringCliException("Could not find file " + file);
		}
		if (!Files.isRegularFile(readmePath)) {
			throw new SpringCliException("The Path " + readmePath + " is not a regular file, can't read");
		}
		try (InputStream stream = Files.newInputStream(readmePath)) {
			String response = StreamUtils.copyToString(stream, UTF_8);
			List<ProjectArtifact> projectArtifacts = createProjectArtifacts(response);
			ProjectArtifactProcessor projectArtifactProcessor = new ProjectArtifactProcessor(projectArtifacts, projectPath, terminalMessage);
			projectArtifactProcessor.process();
		} catch (IOException ex) {
			throw new SpringCliException("Could not read file " + readmePath.toAbsolutePath(), ex);
		}

	}

	private void writeReadMe(ProjectName projectName, String text, Path projectPath, TerminalMessage terminalMessage) {
		Path output = projectPath.resolve("README-ai-" + projectName.getShortPackageName() + ".md");
		if (output.toFile().exists()) {
			try {
				Files.delete(output);
			}
			catch (IOException ex) {
				throw new SpringCliException("Could not delete readme file: " + output.toAbsolutePath(), ex);
			}
		}
		try (Writer writer = new BufferedWriter(new FileWriter(output.toFile()))) {
			writer.write(text);
		}
		catch (IOException ex) {
			throw new SpringCliException("Could not write readme file: " + output.toAbsolutePath(), ex);
		}
		terminalMessage.print("Read the file " + "README-ai-" +
				projectName.getShortPackageName() + ".md for a description of the code.");
	}

	private static Path getProjectPath(String path) {
		Path projectPath;
		if (path == null) {
			projectPath = IoUtils.getWorkingDirectory();
		}
		else {
			projectPath = IoUtils.getProjectPath(path);
		}
		return projectPath;
	}

	@Override
	public String generate(PromptRequest promptRequest) {
		// get api token in file ~/.openai
		Properties properties = PropertyFileUtils.getPropertyFile();
		String apiKey = properties.getProperty("OPEN_AI_API_KEY");
		OpenAiService openAiService = new OpenAiService(apiKey, Duration.of(5, ChronoUnit.MINUTES));

		ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
				.builder()
				.model("gpt-3.5-turbo")
				.temperature(0.3)
				.messages(
						List.of(
								new ChatMessage("system", promptRequest.getSystemPrompt()),
								new ChatMessage("user", promptRequest.getUserPrompt())))
				.build();

		StringBuilder builder = new StringBuilder();
		openAiService.createChatCompletion(chatCompletionRequest)
				.getChoices().forEach(choice -> {
					builder.append(choice.getMessage().getContent());
				});

		return builder.toString();
	}


	private Map<String, String> getContext(String description, ProjectName projectName,  Path path) {
		Map<String, String> context = new HashMap<>();
		context.put("build-tool", "maven");
		context.put("package-name", calculatePackage(projectName.getShortPackageName(), path));
		context.put("spring-project-name", projectName.getSpringProjectName());
		context.put("description", description);
		return context;
	}


	private String calculatePackage(String shortPackageName, Path path) {
		Optional<String> rootPackage = RootPackageFinder.findRootPackage(path.toFile());
		if (rootPackage.isEmpty()) {
			throw new SpringCliException("Could not find root package from path " + path.toAbsolutePath());
		}
		return rootPackage.get() + ".ai." + shortPackageName;
	}


	@NotNull
	private static String getDefaultPackage(String shortPackageName, Path path) {
		Optional<String> rootPackage = RootPackageFinder.findRootPackage(path.toFile());
		if (rootPackage.isEmpty()) {
			throw new SpringCliException("Could not find root package from path " + path.toAbsolutePath());
		}
		return rootPackage.get() + ".ai." + shortPackageName;
	}

	List<ProjectArtifact> createProjectArtifacts(String response) {
		ProjectArtifactCreator projectArtifactCreator = new ProjectArtifactCreator();
		List<ProjectArtifact> projectArtifacts = projectArtifactCreator.create(response);
		return projectArtifacts;
	}

	private PromptRequest createPrompt(Map<String, String> context) {
		String systemPrompt = getPrompt(context, "system");
		String userPrompt = getPrompt(context, "user");
		return new PromptRequest(systemPrompt, userPrompt);
	}

	private String getPrompt(Map<String, String> context, String promptType) {
		ClassPathResource promptResource =
				new ClassPathResource("/org/springframework/cli/merger/ai/openai-" + promptType + "-prompt.txt");
		try {
			String promptRaw = StreamUtils.copyToString(promptResource.getInputStream(), UTF_8);
			return this.handlebarsTemplateEngine.process(promptRaw, context);
		}
		catch (IOException e) {
			throw new SpringCliException("Could not read resource " + promptResource);
		}
	}


}

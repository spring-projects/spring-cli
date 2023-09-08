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


package org.springframework.cli.runtime.engine.actions.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.openrewrite.Recipe;
import org.openrewrite.maven.AddManagedDependency;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependencyManagement;
import org.springframework.cli.runtime.engine.templating.TemplateEngine;
import org.springframework.cli.util.MavenDependencyReader;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.util.StringUtils;

public class InjectMavenDependencyManagementActionHandler extends AbstractInjectMavenActionHandler {

    public InjectMavenDependencyManagementActionHandler(TemplateEngine templateEngine, Map<String, Object> model, Path cwd, TerminalMessage terminalMessage) {
        super(templateEngine, model, cwd, terminalMessage);
    }

    public void execute(InjectMavenDependencyManagement injectMavenDependencyManagement) {
        Path pomPath = getPomPath();
        String text = getTextToUse(injectMavenDependencyManagement.getText(), "Inject Maven Dependency Management");
        if (!StringUtils.hasText(text)) {
            throw new SpringCliException("Inject Maven Dependency Management action does not have a value in the 'text:' field.");
        }

        MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
        String[] mavenDependencies = mavenDependencyReader.parseMavenSection(text);
        for (String mavenDependency : mavenDependencies) {
            Dependency d = Dependency.from(mavenDependency);
			Recipe addMavenManagedDependency = new AddManagedDependency(
					d.groupId(),
					d.artifactId(),
					d.version(),
					d.scope(),
					d.type(),
					d.classifier(),
					d.versionPattern(),
					d.releasesOnly(),
					d.onlyIfUsing(),
					d.addToRootPom()
			);
			//InjectTextManagedDependencyRecipe injectTextManagedDependencyRecipe = new InjectTextManagedDependencyRecipe(mavenDependency);
            runRecipe(pomPath, addMavenManagedDependency);
        }
    }

    static class Dependency {

		private final String groupId;
		private final String artifactId;
		private String version;
		private String scope;
		private String type;
		private String classifier;
		private String versionPattern;
		private Boolean releasesOnly;
		private String onlyIfUsing;
		private Boolean addToRootPom;

		Dependency(String dependencyXmlSnippet) {
            try {
				ObjectMapper om = new XmlMapper();
				JsonNode e = om.readTree(dependencyXmlSnippet);

//					for(int i =0; i < dependencyTag.getLength(); i++) {
							groupId = extract(e, "groupId");
							artifactId = extract(e, "artifactId");
							version = extract(e, "version");
							scope = extract(e, "scope");
							type = extract(e, "type");
							classifier = extract(e, "classifier");
							versionPattern = extract(e, "versionPattern");
							releasesOnly = false;
							onlyIfUsing = null;
							addToRootPom = false;
//					}


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

		private static String extract(JsonNode e, String tag) {
			if(e == null) {
				throw new IllegalArgumentException("Tag %s in Element %e was not found.".formatted(tag, e.asText()));
			}
			return e.get(tag) != null ? e.get(tag).asText() : null;
		}

		public static Dependency from(String mavenDependency) {
			return new Dependency(mavenDependency);
        }

		public String groupId() {
			return groupId;
		}

		public String artifactId() {
			return artifactId;
		}

		public String version() {
			return version;
		}

		public String scope() {
			return scope;
		}

		public String type() {
			return type;
		}

		public String classifier() {
			return classifier;
		}

		public String versionPattern() {
			return versionPattern;
		}

		public Boolean releasesOnly() {
			return releasesOnly;
		}

		public String onlyIfUsing() {
			return onlyIfUsing;
		}

		public Boolean addToRootPom() {
			return addToRootPom;
		}
	}
}

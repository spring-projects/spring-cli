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
package org.springframework.up.support;

import java.util.Arrays;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.style.ThemeResolver;
import org.springframework.up.config.UpCliProperties;
import org.springframework.util.StringUtils;

/**
 * Base class for all cli commands.
 *
 * @author Janne Valkealahti
 */
public abstract class AbstractUpCliCommands extends AbstractShellComponent {

    private ObjectProvider<UpCliProperties> cliPropertiesProvider;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);
		this.cliPropertiesProvider = applicationContext.getBeanProvider(UpCliProperties.class);
    }

	protected UpCliProperties getCliProperties() {
		return this.cliPropertiesProvider.getObject();
	}

	// TODO: when shell print commands mature, move those to AbstractShellComponent

	protected void shellPrint(String... text) {
		for (String t : text) {
			getTerminal().writer().println(t);
		}
		shellFlush();
	}

	protected void shellPrint(AttributedString... text) {
		for (AttributedString t : text) {
			getTerminal().writer().println(t.toAnsi(getTerminal()));
		}
		shellFlush();
	}

	protected void shellFlush() {
		getTerminal().writer().flush();
	}

	protected AttributedString styledString(String text, String tag) {
		AttributedStyle style = null;
		if (StringUtils.hasText(tag)) {
			ThemeResolver themeResolver = getThemeResolver();
			String resolvedStyle = themeResolver.resolveTag(tag);
			style = themeResolver.resolveStyle(resolvedStyle);
		}
		return new AttributedString(text, style);
	}

	protected AttributedString join(AttributedString left, AttributedString right) {
		return AttributedString.join(null, Arrays.asList(left, right));
	}
}

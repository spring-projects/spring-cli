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


package org.springframework.cli.util;

import java.util.Arrays;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.shell.style.ThemeResolver;
import org.springframework.util.StringUtils;

/**
 * Utility class to write to the terminal
 */
public class SpringCliTerminal implements org.springframework.cli.util.TerminalMessage {

	private Terminal terminal;

	private ThemeResolver themeResolver;

	public SpringCliTerminal(Terminal terminal, ThemeResolver themeResolver) {
		this.terminal = terminal;
		this.themeResolver = themeResolver;
	}

	public void print(String... text) {
		for (String t : text) {
			this.terminal.writer().println(t);
		}
		shellFlush();
	}

	public void print(AttributedString... text) {
		for (AttributedString t : text) {
			terminal.writer().println(t.toAnsi(terminal));
		}
		shellFlush();
	}

	public void shellFlush() {
		this.terminal.writer().flush();
	}

	public AttributedString styledString(String text, String tag) {
		AttributedStyle style = null;
		if (StringUtils.hasText(tag)) {
			String resolvedStyle = themeResolver.resolveStyleTag(tag);
			style = themeResolver.resolveStyle(resolvedStyle);
		}
		return new AttributedString(text, style);
	}

	public AttributedString join(AttributedString left, AttributedString right) {
		return AttributedString.join(null, Arrays.asList(left, right));
	}

}

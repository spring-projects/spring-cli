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
package org.springframework.cli.util;

import org.jline.utils.AttributedString;

/**
 * Interface printing shell messages.
 *
 * @author Janne Valkealahti
 */
public interface TerminalMessage {

	/**
	 * Print a text.
	 *
	 * @param text a text
	 */
	void shellPrint(String... text);

	/**
	 * Print an attributed text
	 *
	 * @param text a text
	 */
	void shellPrint(AttributedString... text);

	/**
	 * Gets an implementation which does nothing.
	 *
	 * @return a noop implementation
	 */
	static TerminalMessage noop() {
		return new TerminalMessage() {

			@Override
			public void shellPrint(String... text) {
			}

			@Override
			public void shellPrint(AttributedString... text) {
			}
		};
	}
}

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

import java.util.ArrayList;
import java.util.List;

import org.jline.utils.AttributedString;

public class StubTerminalMessage implements TerminalMessage {

	private List<String> printMessages = new ArrayList();

	private List<String> printAttributedMessages = new ArrayList();

	@Override
	public void print(String... text) {
		for (String s : text) {
			this.printMessages.add(s);
		}

	}

	@Override
	public void print(AttributedString... text) {
		for (AttributedString attributedString : text) {
			this.printAttributedMessages.add(attributedString.toString());
		}
	}

	public void reset() {
		this.printMessages = new ArrayList<>();
		this.printAttributedMessages = new ArrayList<>();
	}

	public List<String> getPrintMessages() {
		return printMessages;
	}

	public List<String> getPrintAttributedMessages() {
		return printAttributedMessages;
	}

}

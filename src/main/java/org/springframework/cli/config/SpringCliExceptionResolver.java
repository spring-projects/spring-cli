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

package org.springframework.cli.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;

import org.jline.terminal.Terminal;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;

public class SpringCliExceptionResolver implements CommandExceptionResolver, ApplicationContextAware, InitializingBean {

	private ApplicationContext applicationContext;

	private ObjectProvider<Terminal> terminalProvider;

	@Override
	public CommandHandlingResult resolve(Exception e) {
		File stackTraceFile = new File(System.getProperty("java.io.tmpdir"), "spring-cli-stacktrace.txt");
		try {
			e.printStackTrace(new PrintStream(stackTraceFile));
		}
		catch (FileNotFoundException ex) {
			shellPrint("Could not write stack trace to file " + stackTraceFile);
			shellPrint(Arrays.toString(ex.getStackTrace()));
		}
		return CommandHandlingResult.of(e.getMessage(), 1);
	}

	private Terminal getTerminal() {
		return terminalProvider.getObject();
	}

	protected void shellPrint(String... text) {
		for (String t : text) {
			getTerminal().writer().println(t);
		}
		shellFlush();
	}

	protected void shellFlush() {
		getTerminal().writer().flush();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		terminalProvider = applicationContext.getBeanProvider(Terminal.class);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}

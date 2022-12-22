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

import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;

public class SpringCliExceptionResolver implements CommandExceptionResolver {

	@Override
	public CommandHandlingResult resolve(Exception ex) {
		File stackTraceFile = new File(System.getProperty("java.io.tmpdir"), "spring-cli-stacktrace.txt");
		try {
			ex.printStackTrace(new PrintStream(stackTraceFile));
		}
		catch (FileNotFoundException e) {
			System.out.println("Could not write stack trace to file " + stackTraceFile);
			System.out.println(Arrays.toString(e.getStackTrace()));
		}
		return CommandHandlingResult.of(ex.getMessage(), 1);
	}
}

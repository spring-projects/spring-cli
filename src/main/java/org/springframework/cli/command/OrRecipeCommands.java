/*
 * Copyright 2021 - 2023 the original author or authors.
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
package org.springframework.cli.command;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.cli.merger.RecipeRunHandler;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.standard.commands.Help;

/**
 * @author Fabian Krüger
 */
@Command(command = "recipe", group = "OpenRewrite")
public class OrRecipeCommands {
    private final RecipeRunHandler recipeRunHandler;
    private final TerminalMessage terminalMessage;

    public OrRecipeCommands(RecipeRunHandler recipeRunHandler, TerminalMessage terminalMessage) {
        this.recipeRunHandler = recipeRunHandler;
        this.terminalMessage = terminalMessage;
    }

    @Command(command = "apply",
            description = "Apply an OpenRewrite recipe.")
    public void bootAdd(
            @Option(description = "Add to the current project from an existing project by specifying the existing project's name or URL.") String from,
            @Option(description = "Path") String path) {
//        ProjectHandler handler = new ProjectHandler(springCliUserConfig, sourceRepositoryService, terminalMessage);
//        handler.add(from, path);

        printWarning(from);

        recipeRunHandler.run(from, path);
    }

    private void printWarning(String from) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
//        sb.style(sb.style().background(AttributedStyle.WHITE));
        sb.style(sb.style().foreground(AttributedStyle.RED));
        sb.append("""
        
        WARNING: 
        This will build the project from %s and execute its code on this machine.
        This can imply serious security risks. You should only proceed if you trust the code in this repo.
        
        Do you want to proceed (y/n)?
        """.formatted(from));
        sb.append(System.lineSeparator());
        this.terminalMessage.print(sb.toAttributedString());
    }
}

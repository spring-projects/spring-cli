/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.cli.runtime.engine.actions.handlers.json;

import java.util.List;
import java.util.Map;

public class Lsp {

    public interface ChangeOperation {}

    public record Position(int line, int character) {}

    public record Range(Position start, Position end) {}

    public record TextEdit(Range range, String newText, String annotationId) {}

    public record TextDocumentIdentifier(String uri, Integer version) {
        public TextDocumentIdentifier(String uri) {
            this(uri, null);
        }
    }

    public record TextDocumentEdit(TextDocumentIdentifier textDocument, List<TextEdit> edits) implements ChangeOperation {}

    public record CreateFileOptions(boolean overwrite, boolean ignoreIfExists) {}

    public record CreateFile(String kind, String uri, CreateFileOptions options, String annotationId) implements ChangeOperation {
        public CreateFile(String uri, CreateFileOptions options, String annotationId) {
            this("create", uri, options, annotationId);
        }
    }

    public record DeleteFileOptions(boolean recursive, boolean ignoreIfExists) {}

    public record DeleteFile(String kind, String uri, DeleteFileOptions options, String annotationId) implements ChangeOperation {
        public DeleteFile(String uri, DeleteFileOptions options, String annotationId) {
            this("delete", uri, options, annotationId);
        }
    }

    public record ChangeAnnotation(String label, boolean needsConfirmation, String description) {}

    public record WorkspaceEdit(List<ChangeOperation> documentChanges, Map<String, ChangeAnnotation> changeAnnotations) {}
}

package org.springframework.cli.runtime.engine.actions.handlers.json;

public class BadLocationException extends Exception {
    public BadLocationException(Throwable e) {
        super(e);
    }

    public BadLocationException(String message) {
        super(message);
    }

    public BadLocationException() {
        super();
    }

}

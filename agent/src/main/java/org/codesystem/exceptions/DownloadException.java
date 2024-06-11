package org.codesystem.exceptions;

import org.codesystem.AgentApplication;

public class DownloadException extends RuntimeException {
    private final String message;

    public DownloadException(String errorMessage) {
        message = errorMessage;
        AgentApplication.logger.severe(errorMessage);
    }

    @Override
    public String getMessage() {
        return message;
    }
}

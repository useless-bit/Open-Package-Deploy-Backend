package org.codesystem.exceptions;

import org.codesystem.AgentApplication;

public class DownloadException extends RuntimeException {
    public DownloadException(String errorMessage) {
        AgentApplication.logger.severe(errorMessage);
    }
}

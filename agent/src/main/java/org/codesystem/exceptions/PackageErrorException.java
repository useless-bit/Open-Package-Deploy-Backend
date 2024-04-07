package org.codesystem.exceptions;

import org.codesystem.AgentApplication;

public class PackageErrorException extends RuntimeException {
    private final String message;

    public PackageErrorException(String errorMessage) {
        message = errorMessage;
        AgentApplication.logger.severe(errorMessage);
    }

    @Override
    public String getMessage(){
        return message;
    }
}

package org.codesystem.exceptions;

import org.codesystem.AgentApplication;

public class PackageErrorException extends RuntimeException {
    public PackageErrorException(String errorMessage) {
        AgentApplication.logger.severe(errorMessage);
    }
}

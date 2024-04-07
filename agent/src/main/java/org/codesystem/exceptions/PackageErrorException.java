package org.codesystem.exceptions;

import org.codesystem.AgentApplication;
import org.codesystem.utility.SystemExitUtility;

public class PackageErrorException extends RuntimeException {
    public PackageErrorException(String errorMessage) {
        AgentApplication.logger.severe(errorMessage);
    }
}

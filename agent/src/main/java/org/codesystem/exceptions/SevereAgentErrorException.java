package org.codesystem.exceptions;

import org.codesystem.AgentApplication;

public class SevereAgentErrorException extends RuntimeException {
    public SevereAgentErrorException(String errorMessage) {
        AgentApplication.logger.severe(errorMessage);
        System.exit(-1);
    }
}

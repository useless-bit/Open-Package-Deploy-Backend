package org.codesystem.exceptions;

import org.codesystem.AgentApplication;
import org.codesystem.SystemExit;

public class SevereAgentErrorException extends RuntimeException {
    public SevereAgentErrorException(String errorMessage) {
        AgentApplication.logger.severe(errorMessage);
        SystemExit.exit(-1);
    }
}

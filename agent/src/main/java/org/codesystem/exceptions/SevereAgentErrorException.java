package org.codesystem.exceptions;

import org.codesystem.AgentApplication;
import org.codesystem.utility.SystemExitUtility;

public class SevereAgentErrorException extends RuntimeException {
    public SevereAgentErrorException(String errorMessage) {
        AgentApplication.logger.severe(errorMessage);
        SystemExitUtility.exit(-1);
    }
}

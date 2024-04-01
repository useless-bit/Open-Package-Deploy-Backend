package org.codesystem.server.exception;

import org.slf4j.LoggerFactory;

public class CryptoUtilityException extends RuntimeException {

    public CryptoUtilityException(String errorMessage) {
        super();
        LoggerFactory.getLogger(CryptoUtilityException.class).warn(errorMessage);
    }
}

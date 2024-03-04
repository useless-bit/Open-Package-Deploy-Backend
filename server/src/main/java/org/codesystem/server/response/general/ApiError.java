package org.codesystem.server.response.general;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ApiError implements ApiResponse {

    private Instant timestamp;
    private String message;

    private ApiError() {
        timestamp = Instant.now();
    }

    public ApiError(String message) {
        this();
        this.message = message;
    }
}
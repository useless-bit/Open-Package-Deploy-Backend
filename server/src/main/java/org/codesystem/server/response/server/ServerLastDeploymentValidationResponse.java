package org.codesystem.server.response.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class ServerLastDeploymentValidationResponse implements ApiResponse {
    private Instant lastDeploymentValidation;
}

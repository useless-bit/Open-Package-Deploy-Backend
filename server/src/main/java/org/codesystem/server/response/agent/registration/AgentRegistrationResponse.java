package org.codesystem.server.response.agent.registration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@AllArgsConstructor
public class AgentRegistrationResponse implements ApiResponse {
    private String publicKeyBase64;
    private String encryptedValidationToken;
}

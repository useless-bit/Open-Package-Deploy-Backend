package org.codesystem.server.request.agent.registration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentRegistrationRequest {
    private String publicKeyBase64;
    private String name;
    private String authenticationToken;
}

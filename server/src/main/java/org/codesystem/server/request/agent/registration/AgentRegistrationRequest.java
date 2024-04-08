package org.codesystem.server.request.agent.registration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AgentRegistrationRequest {
    private String publicKeyBase64;
    private String name;
    private String authenticationToken;
}

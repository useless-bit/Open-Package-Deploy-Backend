package org.codesystem.server.request.agent.registration;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegistrationRequest {
    private String publicKeyBase64;
    private String name;
    private String authenticationToken;
}

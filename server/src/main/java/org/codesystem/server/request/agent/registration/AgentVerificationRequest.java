package org.codesystem.server.request.agent.registration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentVerificationRequest {
    private String publicKeyBase64;
    private String verificationToken;
}

package org.codesystem.server.request.agent.registration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentVerificationRequest {
    private String publicKeyBase64;
    private String verificationToken;
}

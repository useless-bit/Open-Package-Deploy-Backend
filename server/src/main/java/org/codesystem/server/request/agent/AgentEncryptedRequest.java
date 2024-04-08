package org.codesystem.server.request.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgentEncryptedRequest {
    private String publicKeyBase64;
    private String message;
}


package org.codesystem.server.request.deployment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateNewDeploymentRequest {
    private String agentUUID;
    private String packageUUID;
}

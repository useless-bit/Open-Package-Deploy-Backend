package org.codesystem.server.request.deployment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateNewDeploymentRequest {
    private String agentUUID;
    private String packageUUID;
}

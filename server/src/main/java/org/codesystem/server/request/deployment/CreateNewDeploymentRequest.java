package org.codesystem.server.request.deployment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class CreateNewDeploymentRequest {
    private String agentUUID;
    private String packageUUID;
}

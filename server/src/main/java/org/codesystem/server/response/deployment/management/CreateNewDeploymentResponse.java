package org.codesystem.server.response.deployment.management;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@Setter
@AllArgsConstructor
public class CreateNewDeploymentResponse implements ApiResponse {
    private String deploymentUUID;
}

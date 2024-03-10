package org.codesystem.server.response.deployment.management;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class GetDeploymentResponse implements ApiResponse {
    private String uuid;
    private String agentUUID;
    private String agentName;
    private String packageUUID;
    private String packageName;
    private boolean isDeployed;
    private String expectedReturnValue;
    private String returnValue;
    private Instant lastDeploymentTimestamp;

    public GetDeploymentResponse(DeploymentEntity deploymentEntity) {
        this.uuid = deploymentEntity.getUuid();
        this.agentUUID = deploymentEntity.getAgentEntity().getUuid();
        this.agentName = deploymentEntity.getAgentEntity().getName();
        this.packageUUID = deploymentEntity.getPackageEntity().getUuid();
        this.packageName = deploymentEntity.getPackageEntity().getName();
        this.isDeployed = deploymentEntity.isDeployed();
        this.expectedReturnValue = deploymentEntity.getPackageEntity().getExpectedReturnValue();
        this.returnValue = deploymentEntity.getReturnValue();
        this.lastDeploymentTimestamp = deploymentEntity.getLastDeploymentTimestamp();
    }
}

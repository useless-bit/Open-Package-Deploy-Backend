package org.codesystem.server.response.deployment.management;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class DeploymentInfoListResponse implements ApiResponse {
    private List<DeploymentInfoResponse> deployments;

    public DeploymentInfoListResponse(List<DeploymentEntity> deploymentEntities) {
        this.deployments = deploymentEntities.stream()
                .map(DeploymentInfoResponse::new)
                .toList();
    }

}

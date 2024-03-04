package org.codesystem.server.response.deployment.management;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class GetAllDeploymentsResponse implements ApiResponse {
    private List<GetDeploymentResponse> deployments;

    public GetAllDeploymentsResponse(List<DeploymentEntity> deploymentEntities) {
        this.deployments = deploymentEntities.stream()
                .map(GetDeploymentResponse::new)
                .toList();
    }

}

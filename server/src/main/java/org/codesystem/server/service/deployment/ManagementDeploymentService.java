package org.codesystem.server.service.deployment;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.deployment.CreateNewDeploymentRequest;
import org.codesystem.server.response.deployment.management.CreateNewDeploymentResponse;
import org.codesystem.server.response.deployment.management.GetAllDeploymentsResponse;
import org.codesystem.server.response.deployment.management.GetDeploymentResponse;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagementDeploymentService {
    private final DeploymentRepository deploymentRepository;
    private final AgentRepository agentRepository;
    private final PackageRepository packageRepository;

    public ResponseEntity<ApiResponse> getAllPackages() {
        return ResponseEntity.ok().body(new GetAllDeploymentsResponse(deploymentRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> detDeployment(String deploymentUUID) {
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Deployment not found"));
        }
        return ResponseEntity.ok().body(new GetDeploymentResponse(deploymentEntity));
    }

    public ResponseEntity<ApiResponse> createNewDeployment(CreateNewDeploymentRequest createNewDeploymentRequest) {
        //todo: add null checks
        AgentEntity agentEntity = agentRepository.findFirstByUuid(createNewDeploymentRequest.getAgentUUID());
        PackageEntity packageEntity = packageRepository.findFirstByUuid(createNewDeploymentRequest.getPackageUUID());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Agent not found"));
        }
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Package not found"));
        }
        if (packageEntity.getPackageStatusInternal() != PackageStatusInternal.PROCESSED) {
            return ResponseEntity.badRequest().body(new ApiError("Package not available for deployment"));
        }
        if (deploymentRepository.isDeploymentAlreadyPresent(agentEntity.getUuid(), packageEntity.getUuid())) {
            return ResponseEntity.badRequest().body(new ApiError("Deployment already present"));
        }
        if (agentEntity.getOperatingSystem() != packageEntity.getTargetOperatingSystem()) {
            return ResponseEntity.badRequest().body(new ApiError("OS mismatch"));
        } else if (agentEntity.getOperatingSystem() == OperatingSystem.UNKNOWN) {
            return ResponseEntity.badRequest().body(new ApiError("OS invalid "));
        }
        DeploymentEntity deploymentEntity = new DeploymentEntity(agentEntity, packageEntity, createNewDeploymentRequest.getExpectedReturnValue());
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        return ResponseEntity.ok().body(new CreateNewDeploymentResponse(deploymentEntity.getUuid()));
    }

    public ResponseEntity<ApiResponse> deleteDeployment(String deploymentUUID) {
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Deployment not found"));
        }
        deploymentRepository.delete(deploymentEntity);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> getAllPackagesForAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Agent not found"));
        }
        List<DeploymentEntity> deployments = deploymentRepository.findDeploymentsForAgent(agentEntity.getUuid());
        return ResponseEntity.ok().body(new GetAllDeploymentsResponse(deployments));
    }
}

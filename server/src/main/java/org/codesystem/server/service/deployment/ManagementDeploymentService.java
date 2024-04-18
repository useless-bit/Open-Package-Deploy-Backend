package org.codesystem.server.service.deployment;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.GroupRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.deployment.CreateNewDeploymentRequest;
import org.codesystem.server.response.deployment.management.CreateNewDeploymentResponse;
import org.codesystem.server.response.deployment.management.GetAllDeploymentsResponse;
import org.codesystem.server.response.deployment.management.GetDeploymentResponse;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagementDeploymentService {
    private final DeploymentRepository deploymentRepository;
    private final AgentRepository agentRepository;
    private final PackageRepository packageRepository;
    private final GroupRepository groupRepository;

    public ResponseEntity<ApiResponse> getAllDeployments() {
        return ResponseEntity.ok().body(new GetAllDeploymentsResponse(deploymentRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getDeployment(String deploymentUUID) {
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_DEPLOYMENT));
        }
        return ResponseEntity.ok().body(new GetDeploymentResponse(deploymentEntity));
    }

    public ResponseEntity<ApiResponse> createNewDeployment(CreateNewDeploymentRequest createNewDeploymentRequest) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(createNewDeploymentRequest.getAgentUUID());
        PackageEntity packageEntity = packageRepository.findFirstByUuid(createNewDeploymentRequest.getPackageUUID());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        if (packageEntity.getPackageStatusInternal() == PackageStatusInternal.MARKED_AS_DELETED) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.DEPLOYMENT_ERROR_PACKAGE_NOT_AVAILABLE));
        }
        if (agentEntity.getOperatingSystem() == OperatingSystem.UNKNOWN || packageEntity.getTargetOperatingSystem() == OperatingSystem.UNKNOWN) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.DEPLOYMENT_ERROR_INVALID_OS));
        }
        if (agentEntity.getOperatingSystem() != packageEntity.getTargetOperatingSystem()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_OS_MISMATCH));
        }
        if (deploymentRepository.isDeploymentAlreadyPresent(agentEntity.getUuid(), packageEntity.getUuid())) {
            DeploymentEntity deploymentEntity = deploymentRepository.findAllByAgentUUIDAndPackageUUID(agentEntity.getUuid(), packageEntity.getUuid()).get(0);
            if (deploymentEntity.isDirectDeployment()) {
                return ResponseEntity.badRequest().body(new ApiError(Variables.DEPLOYMENT_ERROR_ALREADY_PRESENT));
            } else {
                deploymentEntity.setDirectDeployment(true);
                deploymentEntity = deploymentRepository.save(deploymentEntity);
                return ResponseEntity.ok().body(new CreateNewDeploymentResponse(deploymentEntity.getUuid()));
            }
        }
        DeploymentEntity deploymentEntity = new DeploymentEntity(agentEntity, packageEntity);
        deploymentEntity.setDirectDeployment(true);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        return ResponseEntity.ok().body(new CreateNewDeploymentResponse(deploymentEntity.getUuid()));
    }

    public ResponseEntity<ApiResponse> deleteDeployment(String deploymentUUID) {
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_DEPLOYMENT));
        }
        if (!deploymentEntity.isDirectDeployment()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.DEPLOYMENT_ERROR_THROUGH_GROUP));
        }
        if (groupRepository.isPackageAvailableThroughGroup(deploymentEntity.getAgentEntity().getUuid(), deploymentEntity.getPackageEntity().getUuid())) {
            deploymentEntity.setDirectDeployment(false);
            deploymentRepository.save(deploymentEntity);
            return ResponseEntity.ok().build();
        }
        deploymentRepository.delete(deploymentEntity);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> getAllDeploymentsForAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        List<DeploymentEntity> deployments = deploymentRepository.findDeploymentsForAgent(agentEntity.getUuid());
        return ResponseEntity.ok().body(new GetAllDeploymentsResponse(deployments));
    }

    public ResponseEntity<ApiResponse> resetDeployment(String deploymentUUID) {
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_DEPLOYMENT));
        }
        deploymentRepository.resetDeployment(deploymentUUID);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> resetDeploymentForAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        deploymentRepository.resetDeploymentsForAgent(agentEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> resetDeploymentForPackage(String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        deploymentRepository.resetDeploymentsForPackage(packageEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

package org.codesystem.server.service.agent.communication;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.ServerApplication;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.agent.AgentEncryptedRequest;
import org.codesystem.server.request.agent.communication.AgentCheckForUpdateRequest;
import org.codesystem.server.request.agent.communication.AgentDeploymentResultRequest;
import org.codesystem.server.response.agent.communication.AgentCheckForUpdateResponse;
import org.codesystem.server.response.agent.communication.AgentPackageDetailResponse;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.utility.RequestValidator;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentCommunicationService {
    private final AgentRepository agentRepository;
    private final DeploymentRepository deploymentRepository;
    private final RequestValidator requestValidator;
    private final ServerRepository serverRepository;
    private final ResourceLoader resourceLoader;

    public ResponseEntity<ApiResponse> checkForUpdates(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestValidator.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid Request"));
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid Key"));
        }

        updateAgent(agentEntity, new AgentCheckForUpdateRequest(request));
        List<DeploymentEntity> deploymentEntities = deploymentRepository.findAvailableDeployments_test(agentEntity.getUuid(), Instant.now().minus(6, ChronoUnit.HOURS));
        boolean deploymentAvailable = !deploymentEntities.isEmpty();
        String checksum = serverRepository.findAll().get(0).getAgentChecksum();
        return ResponseEntity.ok().body(requestValidator.generateAgentEncryptedResponse(new AgentCheckForUpdateResponse(60, deploymentAvailable, checksum).toJsonObject(), agentEntity));
    }

    private void updateAgent(AgentEntity agentEntity, AgentCheckForUpdateRequest agentCheckForUpdateRequest) {
        agentEntity.setOperatingSystem(agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystem());
        agentEntity.setOperatingSystemFamily(agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystemFamily());
        agentEntity.setOperatingSystemArchitecture(agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystemArchitecture());
        agentEntity.setOperatingSystemVersion(agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystemVersion());
        agentEntity.setOperatingSystemCodeName(agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystemCodeName());
        agentEntity.setOperatingSystemBuildNumber(agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystemBuildNumber());
        agentEntity.setCpuName(agentCheckForUpdateRequest.getSystemInformationRequest().getCpuName());
        agentEntity.setCpuArchitecture(agentCheckForUpdateRequest.getSystemInformationRequest().getCpuArchitecture());
        agentEntity.setCpuLogicalCores(agentCheckForUpdateRequest.getSystemInformationRequest().getCpuLogicalCores());
        agentEntity.setCpuPhysicalCores(agentCheckForUpdateRequest.getSystemInformationRequest().getCpuPhysicalCores());
        agentEntity.setCpuSockets(agentCheckForUpdateRequest.getSystemInformationRequest().getCpuSockets());
        agentEntity.setMemory(agentCheckForUpdateRequest.getSystemInformationRequest().getMemory());

        agentEntity.setLastConnectionTime(Instant.now());
        agentRepository.save(agentEntity);
    }

    public ResponseEntity<byte[]> getAgent(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestValidator.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = resourceLoader.getResource("classpath:agent/Agent.jar");

        try {
            return ResponseEntity.ok().body(resource.getContentAsByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<FileSystemResource> getPackage(AgentEncryptedRequest agentEncryptedRequest, String deploymentUUID) {
        JSONObject request = requestValidator.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().build();
        }

        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null || deploymentEntity.isDeployed()
                || (deploymentEntity.getLastDeploymentTimestamp() != null && !deploymentEntity.getLastDeploymentTimestamp().isBefore(Instant.now().minus(6, ChronoUnit.HOURS)))) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Paths.get(ServerApplication.PACKAGE_LOCATION + deploymentEntity.getPackageEntity().getUuid());
        return ResponseEntity.ok().body(new FileSystemResource(new File(String.valueOf(path))));
    }

    public ResponseEntity<ApiResponse> getPackageDetails(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestValidator.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().build();
        }

        List<DeploymentEntity> deploymentEntities = deploymentRepository.findAvailableDeployments_test(agentEntity.getUuid(), Instant.now().minus(6, ChronoUnit.HOURS));
        if (deploymentEntities.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        DeploymentEntity deploymentEntity = deploymentEntities.get(0);
        return ResponseEntity.ok().body(requestValidator.generateAgentEncryptedResponse(new AgentPackageDetailResponse(deploymentEntity).toJsonObject(), agentEntity));
    }

    public ResponseEntity<ApiResponse> sendDeploymentResult(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestValidator.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().build();
        }

        AgentDeploymentResultRequest agentDeploymentResultRequest = new AgentDeploymentResultRequest(request);
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(agentDeploymentResultRequest.getDeploymentUUID());

        if (deploymentEntity == null || deploymentEntity.isDeployed()
                || (deploymentEntity.getLastDeploymentTimestamp() != null && !deploymentEntity.getLastDeploymentTimestamp().isBefore(Instant.now().minus(6, ChronoUnit.HOURS)))) {
            return ResponseEntity.badRequest().build();
        }

        if (agentDeploymentResultRequest.getResultCode().startsWith("AGENT-DEPLOYMENT-ERROR")) {
            deploymentEntity.setDeployed(false);
        } else if (deploymentEntity.getExpectedReturnValue() == null || deploymentEntity.getExpectedReturnValue().isBlank()) {
            deploymentEntity.setDeployed(true);
        } else
            deploymentEntity.setDeployed(agentDeploymentResultRequest.getResultCode().equals(deploymentEntity.getExpectedReturnValue()));

        deploymentEntity.setLastDeploymentTimestamp(Instant.now());
        deploymentEntity.setReturnValue(agentDeploymentResultRequest.getResultCode());
        deploymentRepository.save(deploymentEntity);
        return ResponseEntity.ok().build();
    }
}

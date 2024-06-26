package org.codesystem.server.service.agent.communication;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.ServerApplication;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
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
import org.codesystem.server.service.server.LogService;
import org.codesystem.server.utility.RequestUtility;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
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
    private final RequestUtility requestUtility;
    private final ServerRepository serverRepository;
    private final ResourceLoader resourceLoader;
    private final LogService logService;


    public ResponseEntity<ApiResponse> checkForUpdates(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestUtility.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_KEY));
        }

        AgentCheckForUpdateRequest agentCheckForUpdateRequest = new AgentCheckForUpdateRequest(request);
        if (agentCheckForUpdateRequest.getSystemInformationRequest() != null) {
            String updateAgentResponse = updateAgent(agentEntity, agentCheckForUpdateRequest);
            if (updateAgentResponse != null) {
                return ResponseEntity.badRequest().body(new ApiError(updateAgentResponse));
            }
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        List<DeploymentEntity> deploymentEntities = deploymentRepository.findAvailableDeployments(agentEntity.getUuid(), Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        boolean deploymentAvailable = !deploymentEntities.isEmpty();
        return ResponseEntity.ok().body(requestUtility.generateAgentEncryptedResponse(new AgentCheckForUpdateResponse(serverEntity.getAgentUpdateInterval(), deploymentAvailable, serverEntity.getAgentChecksum()).toJsonObject(), agentEntity));
    }

    private String updateAgent(AgentEntity agentEntity, AgentCheckForUpdateRequest agentCheckForUpdateRequest) {

        if (agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystem() == OperatingSystem.UNKNOWN) {
            return Variables.AGENT_ERROR_INVALID_OS;
        }

        if (agentEntity.getOperatingSystem() != OperatingSystem.UNKNOWN && agentEntity.getOperatingSystem() != agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystem()) {
            logService.addEntry(Severity.ERROR, "The Agent '" + agentEntity.getName() + " | " + agentEntity.getUuid() + "' tried to change the Operating System from: " + agentEntity.getOperatingSystem() + " to: " + agentCheckForUpdateRequest.getSystemInformationRequest().getOperatingSystem());
            return Variables.AGENT_ERROR_CHANGING_OS;
        }

        agentEntity.setAgentChecksum(agentCheckForUpdateRequest.getAgentChecksum());

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
        return null;
    }

    public ResponseEntity<byte[]> getAgent(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestUtility.validateRequest(agentEncryptedRequest);
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
        } catch (Exception e) {
            logService.addEntry(Severity.ERROR, "Failed to serve Agent-Update: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<FileSystemResource> getPackage(AgentEncryptedRequest agentEncryptedRequest, String deploymentUUID) {
        JSONObject request = requestUtility.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().build();
        }

        ServerEntity serverEntity = serverRepository.findAll().get(0);
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        if (deploymentEntity == null || deploymentEntity.isDeployed() || deploymentEntity.getPackageEntity().getPackageStatusInternal() != PackageStatusInternal.PROCESSED
                || (deploymentEntity.getLastDeploymentTimestamp() != null && !deploymentEntity.getLastDeploymentTimestamp().isBefore(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS)))) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Paths.get(ServerApplication.PACKAGE_LOCATION + deploymentEntity.getPackageEntity().getUuid());
        return ResponseEntity.ok().body(new FileSystemResource(new File(String.valueOf(path))));
    }

    public ResponseEntity<ApiResponse> getPackageDetails(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestUtility.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_KEY));
        }

        ServerEntity serverEntity = serverRepository.findAll().get(0);
        List<DeploymentEntity> deploymentEntities = deploymentRepository.findAvailableDeployments(agentEntity.getUuid(), Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        if (deploymentEntities.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_DEPLOYMENT_AVAILABLE));
        }

        DeploymentEntity deploymentEntity = deploymentEntities.get(0);
        return ResponseEntity.ok().body(requestUtility.generateAgentEncryptedResponse(new AgentPackageDetailResponse(deploymentEntity).toJsonObject(), agentEntity));
    }

    public ResponseEntity<ApiResponse> sendDeploymentResult(AgentEncryptedRequest agentEncryptedRequest) {
        JSONObject request = requestUtility.validateRequest(agentEncryptedRequest);
        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_KEY));
        }

        AgentDeploymentResultRequest agentDeploymentResultRequest = new AgentDeploymentResultRequest(request);
        DeploymentEntity deploymentEntity = deploymentRepository.findFirstByUuid(agentDeploymentResultRequest.getDeploymentUUID());

        ServerEntity serverEntity = serverRepository.findAll().get(0);
        if (deploymentEntity == null || deploymentEntity.isDeployed() || deploymentEntity.getPackageEntity().getPackageStatusInternal() != PackageStatusInternal.PROCESSED
                || (deploymentEntity.getLastDeploymentTimestamp() != null && !deploymentEntity.getLastDeploymentTimestamp().isBefore(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS)))) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_DEPLOYMENT));
        }

        if (agentDeploymentResultRequest.getResultCode() == null || agentDeploymentResultRequest.getResultCode().startsWith(Variables.PACKAGE_AGENT_ERROR_BEGINNING)) {
            deploymentEntity.setDeployed(false);
        } else if (deploymentEntity.getPackageEntity().getExpectedReturnValue() == null || deploymentEntity.getPackageEntity().getExpectedReturnValue().isBlank()) {
            deploymentEntity.setDeployed(true);
        } else
            deploymentEntity.setDeployed(agentDeploymentResultRequest.getResultCode().equals(deploymentEntity.getPackageEntity().getExpectedReturnValue()));

        deploymentEntity.setLastDeploymentTimestamp(Instant.now());
        deploymentEntity.setReturnValue(agentDeploymentResultRequest.getResultCode());
        deploymentRepository.save(deploymentEntity);
        return ResponseEntity.ok().build();
    }
}

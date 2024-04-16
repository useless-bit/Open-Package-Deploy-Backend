package org.codesystem.server.service.server;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.server.GroupDeploymentRefreshIntervalRequest;
import org.codesystem.server.request.server.InstallRetryIntervalRequest;
import org.codesystem.server.request.server.UpdateIntervalRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.server.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManagementServerService {
    private final ServerRepository serverRepository;
    private final LogService logService;

    public ResponseEntity<ApiResponse> getRegistrationToken() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetRegistrationTokenResponse(serverEntity.getAgentRegistrationToken()));
    }

    public ResponseEntity<ApiResponse> updateRegistrationToken() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        String newToken = UUID.randomUUID() + "-" + (UUID.randomUUID());
        serverEntity.setAgentRegistrationToken(newToken);
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Agent registration token got updated");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getUpdateInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetUpdateIntervalResponse(serverEntity.getAgentUpdateInterval()));
    }

    public ResponseEntity<ApiResponse> setUpdateInterval(UpdateIntervalRequest updateIntervalRequest) {
        if (updateIntervalRequest == null || updateIntervalRequest.getUpdateInterval() == null || updateIntervalRequest.getUpdateInterval() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_INTERVAL));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentUpdateInterval(updateIntervalRequest.getUpdateInterval());
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Agent update interval updated from: " + serverEntity.getAgentUpdateInterval() + " to: " + updateIntervalRequest.getUpdateInterval());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getInstallRetryInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetInstallRetryIntervalResponse(serverEntity.getAgentInstallRetryInterval()));
    }

    public ResponseEntity<ApiResponse> setInstallRetryInterval(InstallRetryIntervalRequest installRetryIntervalRequest) {
        if (installRetryIntervalRequest == null || installRetryIntervalRequest.getInstallRetryInterval() == null || installRetryIntervalRequest.getInstallRetryInterval() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_INTERVAL));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentInstallRetryInterval(installRetryIntervalRequest.getInstallRetryInterval());
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Agent update interval updated from: " + serverEntity.getAgentUpdateInterval() + " to: " + installRetryIntervalRequest.getInstallRetryInterval());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getAgentChecksum() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetAgentChecksumResponse(serverEntity.getAgentChecksum()));
    }

    public ResponseEntity<ApiResponse> getGroupDeploymentRefreshInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetGroupDeploymentRefreshIntervalResponse(serverEntity.getGroupDeploymentRefreshInterval()));
    }

    public ResponseEntity<ApiResponse> setGroupDeploymentRefreshInterval(GroupDeploymentRefreshIntervalRequest groupDeploymentRefreshIntervalRequest) {
        if (groupDeploymentRefreshIntervalRequest == null || groupDeploymentRefreshIntervalRequest.getGroupDeploymentRefreshInterval() == null || groupDeploymentRefreshIntervalRequest.getGroupDeploymentRefreshInterval() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_INTERVAL));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setGroupDeploymentRefreshInterval(groupDeploymentRefreshIntervalRequest.getGroupDeploymentRefreshInterval());
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Group-Deployment refresh interval updated from: " + serverEntity.getGroupDeploymentRefreshInterval() + " to: " + groupDeploymentRefreshIntervalRequest.getGroupDeploymentRefreshInterval());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> resetGroupDeploymentRefreshInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setLastGroupDeploymentRefresh(null);
        serverRepository.save(serverEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

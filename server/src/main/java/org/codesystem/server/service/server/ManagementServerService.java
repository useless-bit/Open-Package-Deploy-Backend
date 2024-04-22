package org.codesystem.server.service.server;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.server.ServerDeploymentValidationRequest;
import org.codesystem.server.request.server.ServerAgentInstallRetryRequest;
import org.codesystem.server.request.server.ServerAgentUpdateIntervalRequest;
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
    private static final String LOG_TO = " to: ";
    private final ServerRepository serverRepository;
    private final LogService logService;

    public ResponseEntity<ApiResponse> getRegistrationToken() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new ServerRegistrationTokenResponse(serverEntity.getAgentRegistrationToken()));
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
        return ResponseEntity.status(HttpStatus.OK).body(new ServerUpdateIntervalResponse(serverEntity.getAgentUpdateInterval()));
    }

    public ResponseEntity<ApiResponse> setUpdateInterval(ServerAgentUpdateIntervalRequest serverAgentUpdateIntervalRequest) {
        if (serverAgentUpdateIntervalRequest == null || serverAgentUpdateIntervalRequest.getUpdateInterval() == null || serverAgentUpdateIntervalRequest.getUpdateInterval() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_INTERVAL));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentUpdateInterval(serverAgentUpdateIntervalRequest.getUpdateInterval());
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Agent update interval updated from: " + serverEntity.getAgentUpdateInterval() + LOG_TO + serverAgentUpdateIntervalRequest.getUpdateInterval());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getInstallRetryInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new ServerInstallRetryResponse(serverEntity.getAgentInstallRetryInterval()));
    }

    public ResponseEntity<ApiResponse> setInstallRetryInterval(ServerAgentInstallRetryRequest serverAgentInstallRetryRequest) {
        if (serverAgentInstallRetryRequest == null || serverAgentInstallRetryRequest.getInstallRetryInterval() == null || serverAgentInstallRetryRequest.getInstallRetryInterval() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_INTERVAL));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentInstallRetryInterval(serverAgentInstallRetryRequest.getInstallRetryInterval());
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Agent update interval updated from: " + serverEntity.getAgentUpdateInterval() + LOG_TO + serverAgentInstallRetryRequest.getInstallRetryInterval());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getAgentChecksum() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new ServerAgentChecksumResponse(serverEntity.getAgentChecksum()));
    }

    public ResponseEntity<ApiResponse> getDeploymentValidationInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new ServerDeyplomentValidationIntervalResponse(serverEntity.getDeploymentValidationInterval()));
    }

    public ResponseEntity<ApiResponse> setDeploymentValidationInterval(ServerDeploymentValidationRequest serverDeploymentValidationRequest) {
        if (serverDeploymentValidationRequest == null || serverDeploymentValidationRequest.getDeploymentValidationInterval() == null || serverDeploymentValidationRequest.getDeploymentValidationInterval() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_INTERVAL));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setDeploymentValidationInterval(serverDeploymentValidationRequest.getDeploymentValidationInterval());
        serverRepository.save(serverEntity);
        logService.addEntry(Severity.INFO, "Group-Deployment refresh interval updated from: " + serverEntity.getDeploymentValidationInterval() + LOG_TO + serverDeploymentValidationRequest.getDeploymentValidationInterval());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> resetDeploymentValidationInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setLastDeploymentValidation(null);
        serverRepository.save(serverEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getLastDeploymentValidationTimestamp() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new ServerLastDeploymentValidationResponse(serverEntity.getLastDeploymentValidation()));
    }
}

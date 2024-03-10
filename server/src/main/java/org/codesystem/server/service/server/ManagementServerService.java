package org.codesystem.server.service.server;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.server.InstallRetryIntervalRequest;
import org.codesystem.server.request.server.UpdateIntervalRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.server.GetInstallRetryIntervalResponse;
import org.codesystem.server.response.server.GetRegistrationTokenResponse;
import org.codesystem.server.response.server.GetUpdateIntervalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManagementServerService {
    private final ServerRepository serverRepository;
    public ResponseEntity<ApiResponse> getRegistrationToken() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetRegistrationTokenResponse(serverEntity.getAgentRegistrationToken()));
    }

    public ResponseEntity<ApiResponse> updateRegistrationToken() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        String newToken = UUID.randomUUID() + "-" + (UUID.randomUUID());
        serverEntity.setAgentRegistrationToken(newToken);
        serverRepository.save(serverEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getUpdateInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetUpdateIntervalResponse(serverEntity.getAgentUpdateInterval()));
    }

    public ResponseEntity<ApiResponse> setUpdateInterval(UpdateIntervalRequest updateIntervalRequest) {
        if (updateIntervalRequest == null || updateIntervalRequest.getUpdateInterval() == null || updateIntervalRequest.getUpdateInterval() <= 0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("Invalid update interval"));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentUpdateInterval(updateIntervalRequest.getUpdateInterval());
        serverRepository.save(serverEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> getInstallRetryInterval() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        return ResponseEntity.status(HttpStatus.OK).body(new GetInstallRetryIntervalResponse(serverEntity.getAgentInstallRetryInterval()));
    }

    public ResponseEntity<ApiResponse> setInstallRetryInterval(InstallRetryIntervalRequest installRetryIntervalRequest) {
        if (installRetryIntervalRequest == null || installRetryIntervalRequest.getInstallRetryInterval() == null || installRetryIntervalRequest.getInstallRetryInterval() <= 0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("Invalid update interval"));
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentInstallRetryInterval(installRetryIntervalRequest.getInstallRetryInterval());
        serverRepository.save(serverEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

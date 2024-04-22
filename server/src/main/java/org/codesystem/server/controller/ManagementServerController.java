package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.server.ServerDeploymentValidationRequest;
import org.codesystem.server.request.server.ServerAgentInstallRetryRequest;
import org.codesystem.server.request.server.ServerAgentUpdateIntervalRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.server.LogService;
import org.codesystem.server.service.server.ManagementServerService;
import org.codesystem.server.service.server.SystemUsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Management Server")
@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ManagementServerController {
    private final ManagementServerService managementServerService;
    private final LogService logService;
    private final SystemUsageService systemUsageService;

    @GetMapping("registrationToken")
    public ResponseEntity<ApiResponse> getRegistrationToken() {
        return managementServerService.getRegistrationToken();
    }

    @PostMapping("registrationToken")
    public ResponseEntity<ApiResponse> updateRegistrationToken() {
        return managementServerService.updateRegistrationToken();
    }

    @GetMapping("updateInterval")
    public ResponseEntity<ApiResponse> getUpdateInterval() {
        return managementServerService.getUpdateInterval();
    }

    @PatchMapping("updateInterval")
    public ResponseEntity<ApiResponse> setUpdateInterval(@RequestBody ServerAgentUpdateIntervalRequest serverAgentUpdateIntervalRequest) {
        return managementServerService.setUpdateInterval(serverAgentUpdateIntervalRequest);
    }

    @GetMapping("installRetryInterval")
    public ResponseEntity<ApiResponse> getInstallRetryInterval() {
        return managementServerService.getInstallRetryInterval();
    }

    @PatchMapping("installRetryInterval")
    public ResponseEntity<ApiResponse> setInstallRetryInterval(@RequestBody ServerAgentInstallRetryRequest serverAgentInstallRetryRequest) {
        return managementServerService.setInstallRetryInterval(serverAgentInstallRetryRequest);
    }

    @GetMapping("agentChecksum")
    public ResponseEntity<ApiResponse> getAgentChecksum() {
        return managementServerService.getAgentChecksum();
    }

    @GetMapping("log")
    public ResponseEntity<ApiResponse> getAllLogs() {
        return logService.getAllEntries();
    }

    @GetMapping("systemUsage")
    public ResponseEntity<ApiResponse> getLatest30Entries() {
        return systemUsageService.getLatest30Entries();
    }

    @GetMapping("systemUsageFull")
    public ResponseEntity<ApiResponse> getSystemUsage() {
        return systemUsageService.getAllEntries();
    }

    @GetMapping("storage")
    public ResponseEntity<ApiResponse> getStorageInformation() {
        return systemUsageService.getStorageInformation();
    }

    @GetMapping("deploymentValidationInterval")
    public ResponseEntity<ApiResponse> getDeploymentValidationInterval() {
        return managementServerService.getDeploymentValidationInterval();
    }

    @PatchMapping("deploymentValidationInterval")
    public ResponseEntity<ApiResponse> setDeploymentValidationInterval(@RequestBody ServerDeploymentValidationRequest serverDeploymentValidationRequest) {
        return managementServerService.setDeploymentValidationInterval(serverDeploymentValidationRequest);
    }

    @GetMapping("deploymentValidation")
    public ResponseEntity<ApiResponse> getLastDeploymentValidationTimestamp() {
        return managementServerService.getLastDeploymentValidationTimestamp();
    }

    @PatchMapping("deploymentValidation/reset")
    public ResponseEntity<ApiResponse> resetDeploymentValidationInterval() {
        return managementServerService.resetDeploymentValidationInterval();
    }
}

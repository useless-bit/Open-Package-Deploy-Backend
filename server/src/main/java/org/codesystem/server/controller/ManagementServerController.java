package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.server.InstallRetryIntervalRequest;
import org.codesystem.server.request.server.UpdateIntervalRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.server.ManagementServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Management Server")
@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ManagementServerController {
    private final ManagementServerService managementServerService;

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
    public ResponseEntity<ApiResponse> setUpdateInterval(@RequestBody UpdateIntervalRequest updateIntervalRequest) {
        return managementServerService.setUpdateInterval(updateIntervalRequest);
    }

    @GetMapping("installRetryInterval")
    public ResponseEntity<ApiResponse> getInstallRetryInterval() {
        return managementServerService.getInstallRetryInterval();
    }

    @PatchMapping("installRetryInterval")
    public ResponseEntity<ApiResponse> setInstallRetryInterval(@RequestBody InstallRetryIntervalRequest installRetryIntervalRequest) {
        return managementServerService.setInstallRetryInterval(installRetryIntervalRequest);
    }
}

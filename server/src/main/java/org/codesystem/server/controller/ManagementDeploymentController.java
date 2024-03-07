package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.deployment.CreateNewDeploymentRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.deployment.ManagementDeploymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Management Deployment")
@RestController
@RequestMapping("/api/deployment")
@RequiredArgsConstructor
public class ManagementDeploymentController {
    private final ManagementDeploymentService managementDeploymentService;

    @GetMapping()
    public ResponseEntity<ApiResponse> getAllDeployments() {
        return managementDeploymentService.getAllPackages();
    }

    @GetMapping("{deploymentUUID}")
    public ResponseEntity<ApiResponse> getDeployment(@PathVariable String deploymentUUID) {
        return managementDeploymentService.detDeployment(deploymentUUID);
    }
    @GetMapping("agent/{agentUUID}")
    public ResponseEntity<ApiResponse> getAllDeploymentsForAgent(@PathVariable String agentUUID) {
        return managementDeploymentService.getAllPackagesForAgent(agentUUID);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse> createNewDeployment(@RequestBody CreateNewDeploymentRequest createNewDeploymentRequest) {
        return managementDeploymentService.createNewDeployment(createNewDeploymentRequest);
    }

    @DeleteMapping("{deploymentUUID}")
    public ResponseEntity<ApiResponse> deleteDeployment(@PathVariable String deploymentUUID) {
        return managementDeploymentService.deleteDeployment(deploymentUUID);
    }

    @PatchMapping("{deploymentUUID}")
    public ResponseEntity<ApiResponse> resetDeployment(@PathVariable String deploymentUUID) {
        return managementDeploymentService.deleteDeployment(deploymentUUID);
    }
}

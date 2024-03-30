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
        return managementDeploymentService.getAllDeployments();
    }

    @GetMapping("{deploymentUUID}")
    public ResponseEntity<ApiResponse> getDeployment(@PathVariable String deploymentUUID) {
        return managementDeploymentService.getDeployment(deploymentUUID);
    }

    @GetMapping("agent/{agentUUID}")
    public ResponseEntity<ApiResponse> getAllDeploymentsForAgent(@PathVariable String agentUUID) {
        return managementDeploymentService.getAllDeploymentsForAgent(agentUUID);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse> createNewDeployment(@RequestBody CreateNewDeploymentRequest createNewDeploymentRequest) {
        return managementDeploymentService.createNewDeployment(createNewDeploymentRequest);
    }

    @DeleteMapping("{deploymentUUID}")
    public ResponseEntity<ApiResponse> deleteDeployment(@PathVariable String deploymentUUID) {
        return managementDeploymentService.deleteDeployment(deploymentUUID);
    }

    @PatchMapping("reset/deployment/{deploymentUUID}")
    public ResponseEntity<ApiResponse> resetDeployment(@PathVariable String deploymentUUID) {
        return managementDeploymentService.resetDeployment(deploymentUUID);
    }

    @PatchMapping("reset/agent/{agentUUID}")
    public ResponseEntity<ApiResponse> resetDeploymentForAgent(@PathVariable String agentUUID) {
        return managementDeploymentService.resetDeploymentForAgent(agentUUID);
    }

    @PatchMapping("reset/package/{packageUUID}")
    public ResponseEntity<ApiResponse> resetDeploymentForPackage(@PathVariable String packageUUID) {
        return managementDeploymentService.resetDeploymentForPackage(packageUUID);
    }
}

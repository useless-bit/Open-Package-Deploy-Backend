package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.agent.AgentEncryptedRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.agent.communication.AgentCommunicationService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Agent Communication")
@RestController
@RequestMapping("/api/agent/communication")
@RequiredArgsConstructor
public class AgentCommunicationController {
    private final AgentCommunicationService agentCommunicationService;

    @PostMapping("checkForUpdates")
    public ResponseEntity<ApiResponse> addNewAgent(@RequestBody AgentEncryptedRequest agentEncryptedRequest) {
        return agentCommunicationService.checkForUpdates(agentEncryptedRequest);
    }

    @PostMapping("package")
    public ResponseEntity<ApiResponse> getPackageDetails(@RequestBody AgentEncryptedRequest agentEncryptedRequest){
        return agentCommunicationService.getPackageDetails(agentEncryptedRequest);
    }

    @PostMapping(value = "agent", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getAgent(@RequestBody AgentEncryptedRequest agentEncryptedRequest){
        return agentCommunicationService.getAgent(agentEncryptedRequest);
    }

    @PostMapping(value = "package/{deploymentUUID}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<FileSystemResource> getPackage(@RequestBody AgentEncryptedRequest agentEncryptedRequest, @PathVariable String deploymentUUID){
        return agentCommunicationService.getPackage(agentEncryptedRequest, deploymentUUID);
    }


    @PostMapping("deploymentResult")
    public ResponseEntity<ApiResponse> sendDeploymentResult(@RequestBody AgentEncryptedRequest agentEncryptedRequest){
        return agentCommunicationService.sendDeploymentResult(agentEncryptedRequest);
    }
}

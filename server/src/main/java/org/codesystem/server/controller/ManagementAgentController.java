package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.agent.management.AgentUpdateRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.agent.management.ManagementAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Management Agent")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class ManagementAgentController {
    private final ManagementAgentService managementAgentService;

    @GetMapping()
    public ResponseEntity<ApiResponse> getAllAgents() {
        return managementAgentService.getAllAgents();
    }

    @GetMapping("{agentUUID}")
    public ResponseEntity<ApiResponse> getAgent(@PathVariable String agentUUID) {
        return managementAgentService.getAgent(agentUUID);
    }

    @PatchMapping("{agentUUID}")
    public ResponseEntity<ApiResponse> updateAgent(@PathVariable String agentUUID, @RequestBody AgentUpdateRequest agentUpdateRequest) {
        return managementAgentService.updateAgent(agentUUID, agentUpdateRequest);
    }

    @DeleteMapping("{agentUUID}")
    public ResponseEntity<ApiResponse> deleteAgent(@PathVariable String agentUUID) {
        return managementAgentService.deleteAgent(agentUUID);
    }
}

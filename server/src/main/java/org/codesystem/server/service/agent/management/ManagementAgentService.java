package org.codesystem.server.service.agent.management;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.request.agent.management.AgentUpdateRequest;
import org.codesystem.server.response.agent.management.AgentInfoListResponse;
import org.codesystem.server.response.agent.management.AgentInfoResponse;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.server.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagementAgentService {
    private final AgentRepository agentRepository;
    private final DeploymentRepository deploymentRepository;
    private final LogService logService;

    public ResponseEntity<ApiResponse> getAllAgents() {
        return ResponseEntity.ok().body(new AgentInfoListResponse(agentRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        return ResponseEntity.ok().body(new AgentInfoResponse(agentEntity));
    }

    public ResponseEntity<ApiResponse> updateAgent(String agentUUID, AgentUpdateRequest agentUpdateRequest) {
        if (agentUpdateRequest.getName() == null || agentUpdateRequest.getName().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }

        agentEntity.setName(agentUpdateRequest.getName().trim());
        agentRepository.save(agentEntity);
        return ResponseEntity.ok().build();

    }

    public ResponseEntity<ApiResponse> deleteAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        deploymentRepository.deleteAll(deploymentRepository.findDeploymentsForAgent(agentEntity.getUuid()));
        agentRepository.delete(agentEntity);
        logService.addEntry(Severity.INFO, "Successfully deleted Agent: " + agentEntity.getName() + " | " + agentEntity.getUuid());
        return ResponseEntity.ok().build();

    }
}

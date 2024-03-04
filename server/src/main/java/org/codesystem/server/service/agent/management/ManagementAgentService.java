package org.codesystem.server.service.agent.management;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.request.agent.management.AgentUpdateRequest;
import org.codesystem.server.response.agent.management.GetAgentResponse;
import org.codesystem.server.response.agent.management.GetAllAgentsResponse;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagementAgentService {
    private final AgentRepository agentRepository;
    private final DeploymentRepository deploymentRepository;

    public ResponseEntity<ApiResponse> getAllAgents() {
        return ResponseEntity.ok().body(new GetAllAgentsResponse(agentRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Agent not found"));
        }
        return ResponseEntity.ok().body(new GetAgentResponse(agentEntity));
    }

    public ResponseEntity<ApiResponse> updateAgent(String agentUUID, AgentUpdateRequest agentUpdateRequest) {
        //todo: add null checks
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Agent not found"));
        }
        if (agentUpdateRequest.getName().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError("Name cannot be empty"));
        }

        agentEntity.setName(agentUpdateRequest.getName());
        agentRepository.save(agentEntity);
        return ResponseEntity.ok().build();

    }

    public ResponseEntity<ApiResponse> deleteAgent(String agentUUID) {
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Agent not found"));
        }
        deploymentRepository.deleteAll(deploymentRepository.findDeploymentsForAgent(agentEntity.getUuid()));
        agentRepository.delete(agentEntity);
        return ResponseEntity.ok().build();

    }
}

package org.codesystem.server.response.agent.management;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GetAllAgentsResponse implements ApiResponse {
    private final List<GetAgentResponse> agents;

    public GetAllAgentsResponse(List<AgentEntity> agentEntities) {
        this.agents = agentEntities.stream()
                .map(GetAgentResponse::new)
                .toList();
    }
}

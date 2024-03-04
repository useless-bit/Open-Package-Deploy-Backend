package org.codesystem.server.response.agent.management;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class GetAllAgentsResponse implements ApiResponse {
    private List<GetAgentResponse> agents;

    public GetAllAgentsResponse(List<AgentEntity> agentEntities) {
        this.agents = agentEntities.stream()
                .map(GetAgentResponse::new)
                .toList();
    }
}

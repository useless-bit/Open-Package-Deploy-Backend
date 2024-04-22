package org.codesystem.server.response.agent.management;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class AgentInfoListResponse implements ApiResponse {
    private final List<AgentInfoResponse> agents;

    public AgentInfoListResponse(List<AgentEntity> agentEntities) {
        this.agents = agentEntities.stream()
                .map(AgentInfoResponse::new)
                .toList();
    }
}

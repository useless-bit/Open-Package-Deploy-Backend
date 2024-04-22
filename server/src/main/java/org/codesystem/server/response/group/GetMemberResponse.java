package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.response.general.ApiResponse;

@Getter
public class GetMemberResponse implements ApiResponse {
    private final String uuid;
    private final String name;

    public GetMemberResponse(AgentEntity agentEntity) {
        this.uuid = agentEntity.getUuid();
        this.name = agentEntity.getName();
    }
}
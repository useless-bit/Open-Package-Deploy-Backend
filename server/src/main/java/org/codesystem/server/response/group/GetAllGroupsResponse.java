package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.response.agent.management.GetAgentResponse;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GetAllGroupsResponse implements ApiResponse {
    private final List<GetGroupResponse> groups;

    public GetAllGroupsResponse(List<GroupEntity> groupEntities) {
        this.groups = groupEntities.stream()
                .map(GetGroupResponse::new)
                .toList();
    }
}

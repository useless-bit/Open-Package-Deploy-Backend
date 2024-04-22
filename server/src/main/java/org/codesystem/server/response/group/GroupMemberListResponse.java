package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GroupMemberListResponse implements ApiResponse {
    private final List<GroupMemberResponse> members;

    public GroupMemberListResponse(List<AgentEntity> agentEntities) {
        this.members = agentEntities.stream()
                .map(GroupMemberResponse::new)
                .toList();
    }
}

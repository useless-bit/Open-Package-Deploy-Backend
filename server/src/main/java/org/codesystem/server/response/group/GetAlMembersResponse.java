package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GetAlMembersResponse implements ApiResponse {
    private final List<GetMemberResponse> members;

    public GetAlMembersResponse(List<AgentEntity> agentEntities) {
        this.members = agentEntities.stream()
                .map(GetMemberResponse::new)
                .toList();
    }
}

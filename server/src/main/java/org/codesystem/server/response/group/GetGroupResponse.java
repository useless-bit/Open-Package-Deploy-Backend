package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.response.general.ApiResponse;

@Getter
public class GetGroupResponse implements ApiResponse {
    private final String uuid;
    private final String name;

    public GetGroupResponse(GroupEntity groupEntity) {
        this.uuid = groupEntity.getUuid();
        this.name = groupEntity.getName();
    }
}
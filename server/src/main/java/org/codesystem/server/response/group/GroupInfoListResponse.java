package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GroupInfoListResponse implements ApiResponse {
    private final List<GroupInfoResponse> groups;

    public GroupInfoListResponse(List<GroupEntity> groupEntities) {
        this.groups = groupEntities.stream()
                .map(GroupInfoResponse::new)
                .toList();
    }
}

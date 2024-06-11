package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.response.general.ApiResponse;

@Getter
public class GroupInfoResponse implements ApiResponse {
    private final String uuid;
    private final String name;
    private final String description;
    private final OperatingSystem operatingSystem;
    private final int memberCount;
    private final int packageCount;

    public GroupInfoResponse(GroupEntity groupEntity) {
        this.uuid = groupEntity.getUuid();
        this.name = groupEntity.getName();
        this.description = groupEntity.getDescription();
        this.operatingSystem = groupEntity.getOperatingSystem();
        this.memberCount = groupEntity.getMembers().size();
        this.packageCount = groupEntity.getDeployedPackages().size();
    }
}
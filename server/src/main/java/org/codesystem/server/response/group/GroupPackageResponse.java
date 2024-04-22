package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.response.general.ApiResponse;

@Getter
public class GroupPackageResponse implements ApiResponse {
    private final String uuid;
    private final String name;

    public GroupPackageResponse(PackageEntity packageEntity) {
        this.uuid = packageEntity.getUuid();
        this.name = packageEntity.getName();
    }
}
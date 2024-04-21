package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.response.general.ApiResponse;

@Getter
public class GetPackageResponse implements ApiResponse {
    private final String uuid;
    private final String name;

    public GetPackageResponse(PackageEntity packageEntity) {
        this.uuid = packageEntity.getUuid();
        this.name = packageEntity.getName();
    }
}
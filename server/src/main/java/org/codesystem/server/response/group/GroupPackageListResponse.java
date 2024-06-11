package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GroupPackageListResponse implements ApiResponse {
    private final List<GroupPackageResponse> packages;

    public GroupPackageListResponse(List<PackageEntity> packageEntities) {
        this.packages = packageEntities.stream()
                .map(GroupPackageResponse::new)
                .toList();
    }
}

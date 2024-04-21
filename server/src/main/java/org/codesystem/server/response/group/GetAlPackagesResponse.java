package org.codesystem.server.response.group;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
public class GetAlPackagesResponse implements ApiResponse {
    private final List<GetPackageResponse> packages;

    public GetAlPackagesResponse(List<PackageEntity> packageEntities) {
        this.packages = packageEntities.stream()
                .map(GetPackageResponse::new)
                .toList();
    }
}

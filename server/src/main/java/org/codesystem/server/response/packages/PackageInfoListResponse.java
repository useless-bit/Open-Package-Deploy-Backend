package org.codesystem.server.response.packages;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class PackageInfoListResponse implements ApiResponse {
    private List<PackageInfoResponse> packages;

    public PackageInfoListResponse(List<PackageEntity> packageEntities) {
        this.packages = packageEntities.stream()
                .map(PackageInfoResponse::new)
                .toList();
    }
}

package org.codesystem.server.response.packages;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class GetAllPackagesResponse implements ApiResponse {
    private List<GetPackageResponse> packages;

    public GetAllPackagesResponse(List<PackageEntity> packageEntities) {
        this.packages = packageEntities.stream()
                .map(GetPackageResponse::new)
                .toList();
    }
}

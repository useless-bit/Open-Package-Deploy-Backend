package org.codesystem.server.response.server;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.SystemUsageEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class GetSystemUsageResponse implements ApiResponse {
    private List<SystemUsageEntity> systemUsageEntities;

    public GetSystemUsageResponse(List<SystemUsageEntity> systemUsageEntities) {
        this.systemUsageEntities = systemUsageEntities;
    }
}

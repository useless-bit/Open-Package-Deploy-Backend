package org.codesystem.server.response.server;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.SystemUsageEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class ServerSystemUsageResponse implements ApiResponse {
    private List<SystemUsageEntity> systemUsageEntities;

    public ServerSystemUsageResponse(List<SystemUsageEntity> systemUsageEntities) {
        this.systemUsageEntities = systemUsageEntities;
    }
}

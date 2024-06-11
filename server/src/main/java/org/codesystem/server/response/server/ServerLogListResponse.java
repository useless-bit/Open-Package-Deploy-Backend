package org.codesystem.server.response.server;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.LogEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class ServerLogListResponse implements ApiResponse {
    private List<LogEntity> logEntries;

    public ServerLogListResponse(List<LogEntity> logEntities) {
        this.logEntries = logEntities;
    }
}

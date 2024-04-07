package org.codesystem.server.response.server;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.LogEntity;
import org.codesystem.server.response.general.ApiResponse;

import java.util.List;

@Getter
@Setter
public class GetAllLogsResponse implements ApiResponse {
    private List<LogEntity> logEntries;

    public GetAllLogsResponse(List<LogEntity> logEntities) {
        this.logEntries = logEntities;
    }
}

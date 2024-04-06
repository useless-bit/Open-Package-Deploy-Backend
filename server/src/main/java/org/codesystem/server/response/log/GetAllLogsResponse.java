package org.codesystem.server.response.log;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.LogEntity;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.packages.GetPackageResponse;
import org.springframework.data.domain.Sort;

import java.util.List;

@Getter
@Setter
public class GetAllLogsResponse implements ApiResponse {
    private List<LogEntity> logEntries;

    public GetAllLogsResponse(List<LogEntity> logEntities) {
        this.logEntries = logEntities;
    }
}

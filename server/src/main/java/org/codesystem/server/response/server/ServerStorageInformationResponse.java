package org.codesystem.server.response.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@Setter
@AllArgsConstructor
public class ServerStorageInformationResponse implements ApiResponse {
    private long spaceTotal;
    private long spaceAvailable;
}

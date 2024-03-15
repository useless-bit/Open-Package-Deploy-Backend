package org.codesystem.server.response.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@Setter
@AllArgsConstructor
public class GetAgentChecksumResponse implements ApiResponse {
    private String agentChecksum;
}

package org.codesystem.server.response.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@AllArgsConstructor
public class AgentEncryptedResponse implements ApiResponse {
    private String message;
}

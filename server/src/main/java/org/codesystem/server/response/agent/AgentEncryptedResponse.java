package org.codesystem.server.response.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@Setter
@AllArgsConstructor
public class AgentEncryptedResponse implements ApiResponse {
    private String message;
}

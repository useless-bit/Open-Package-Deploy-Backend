package org.codesystem.server.request.agent.communication;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.time.Instant;

@Getter
@Setter
public class AgentCheckForUpdateRequest {
    private SystemInformationRequest systemInformationRequest;

    public AgentCheckForUpdateRequest(JSONObject jsonObject) {
        this.systemInformationRequest = new SystemInformationRequest(jsonObject.getJSONObject("systemInformation"));
    }
}

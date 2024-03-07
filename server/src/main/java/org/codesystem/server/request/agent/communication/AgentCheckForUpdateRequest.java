package org.codesystem.server.request.agent.communication;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
public class AgentCheckForUpdateRequest {
    private SystemInformationRequest systemInformationRequest;
    private String agentChecksum;

    public AgentCheckForUpdateRequest(JSONObject jsonObject) {
        this.systemInformationRequest = new SystemInformationRequest(jsonObject.getJSONObject("systemInformation"));
        this.agentChecksum = jsonObject.getString("agentChecksum");
    }
}

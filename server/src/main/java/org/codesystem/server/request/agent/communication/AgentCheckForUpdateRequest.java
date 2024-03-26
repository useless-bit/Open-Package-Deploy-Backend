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
        if (!jsonObject.isNull("systemInformation")) {
            this.systemInformationRequest = new SystemInformationRequest(jsonObject.getJSONObject("systemInformation"));
        } else {
            this.systemInformationRequest = null;
        }
        if (!jsonObject.isNull("agentChecksum")) {
            this.agentChecksum = jsonObject.getString("agentChecksum");
        } else {
            this.agentChecksum = null;
        }
    }
}

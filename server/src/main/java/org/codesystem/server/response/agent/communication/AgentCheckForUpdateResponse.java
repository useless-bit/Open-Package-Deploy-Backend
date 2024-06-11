package org.codesystem.server.response.agent.communication;

import lombok.AllArgsConstructor;
import org.json.JSONObject;

@AllArgsConstructor
public class AgentCheckForUpdateResponse {
    private int updateInterval;
    private boolean deploymentAvailable;
    private String agentChecksum;

    public JSONObject toJsonObject() {
        return new JSONObject()
                .put("updateInterval", this.updateInterval)
                .put("deploymentAvailable", this.deploymentAvailable)
                .put("agentChecksum", this.agentChecksum);
    }
}

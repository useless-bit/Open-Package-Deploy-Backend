package org.codesystem.server.response.agent.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;
import org.json.JSONObject;

@Getter
@Setter
@AllArgsConstructor
public class AgentCheckForUpdateResponse {
    private int updateCheckTimeout;
    private boolean deploymentAvailable;
    private String agentChecksum;

    public JSONObject toJsonObject() {
        return new JSONObject()
                .put("updateCheckTimeout", this.updateCheckTimeout)
                .put("deploymentAvailable", this.deploymentAvailable)
                .put("agentChecksum", this.agentChecksum);
    }
}

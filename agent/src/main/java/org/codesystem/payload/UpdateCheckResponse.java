package org.codesystem.payload;

import org.json.JSONObject;

public class UpdateCheckResponse {
    private final int updateCheckTimeout;
    private final boolean deploymentAvailable;
    private final String agentChecksum;

    public UpdateCheckResponse(JSONObject jsonObject) {
        this.updateCheckTimeout = jsonObject.getInt("updateCheckTimeout");
        this.deploymentAvailable = jsonObject.getBoolean("deploymentAvailable");
        this.agentChecksum = jsonObject.getString("agentChecksum");
    }

    public int getUpdateCheckTimeout() {
        return updateCheckTimeout;
    }

    public boolean isDeploymentAvailable() {
        return deploymentAvailable;
    }

    public String getAgentChecksum() {
        return agentChecksum;
    }
}

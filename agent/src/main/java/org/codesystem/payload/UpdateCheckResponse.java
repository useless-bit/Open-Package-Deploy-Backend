package org.codesystem.payload;

import org.json.JSONObject;

public class UpdateCheckResponse {
    private final int updateInterval;
    private final boolean deploymentAvailable;
    private final String agentChecksum;

    public UpdateCheckResponse(JSONObject jsonObject) {
        this.updateInterval = jsonObject.getInt("updateInterval");
        this.deploymentAvailable = jsonObject.getBoolean("deploymentAvailable");
        this.agentChecksum = jsonObject.getString("agentChecksum");
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public boolean isDeploymentAvailable() {
        return deploymentAvailable;
    }

    public String getAgentChecksum() {
        return agentChecksum;
    }
}

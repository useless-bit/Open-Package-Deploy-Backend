package org.codesystem.payload;

import org.json.JSONObject;

public class UpdateCheckResponse {
    private final int updateInterval;
    private final boolean deploymentAvailable;
    private final String agentChecksum;

    public UpdateCheckResponse(JSONObject jsonObject) {
        if (jsonObject.isNull("updateInterval") || !(jsonObject.get("updateInterval") instanceof Integer || jsonObject.get("updateInterval") instanceof Long)) {
            this.updateInterval = -1;
        } else {
            this.updateInterval = jsonObject.getInt("updateInterval");
        }
        if (jsonObject.isNull("deploymentAvailable") || !(jsonObject.get("deploymentAvailable") instanceof Boolean)) {
            this.deploymentAvailable = false;
        } else {
            this.deploymentAvailable = jsonObject.getBoolean("deploymentAvailable");
        }
        if (jsonObject.isNull("agentChecksum") || jsonObject.getString("agentChecksum").isBlank()) {
            this.agentChecksum = null;
        } else {
            this.agentChecksum = jsonObject.getString("agentChecksum").trim();
        }
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

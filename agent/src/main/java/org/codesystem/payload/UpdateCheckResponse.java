package org.codesystem.payload;

import org.json.JSONObject;

public class UpdateCheckResponse {
    private static final String UPDATE_INTERVAL = "updateInterval";
    private static final String DEPLOYMENT_AVAILABLE = "deploymentAvailable";
    private static final String AGENT_CHECKSUM = "agentChecksum";
    private final int updateInterval;
    private final boolean deploymentAvailable;
    private final String agentChecksum;

    public UpdateCheckResponse(JSONObject jsonObject) {
        if (jsonObject.isNull(UPDATE_INTERVAL) || !(jsonObject.get(UPDATE_INTERVAL) instanceof Integer || jsonObject.get(UPDATE_INTERVAL) instanceof Long)) {
            this.updateInterval = -1;
        } else {
            this.updateInterval = jsonObject.getInt(UPDATE_INTERVAL);
        }
        if (jsonObject.isNull(DEPLOYMENT_AVAILABLE) || !(jsonObject.get(DEPLOYMENT_AVAILABLE) instanceof Boolean)) {
            this.deploymentAvailable = false;
        } else {
            this.deploymentAvailable = jsonObject.getBoolean(DEPLOYMENT_AVAILABLE);
        }
        if (jsonObject.isNull(AGENT_CHECKSUM) || jsonObject.getString(AGENT_CHECKSUM).isBlank()) {
            this.agentChecksum = null;
        } else {
            this.agentChecksum = jsonObject.getString(AGENT_CHECKSUM).trim();
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

package org.codesystem.server.request.agent.communication;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
public class AgentDeploymentResultRequest {
    private String deploymentUUID;
    private String resultCode;

    public AgentDeploymentResultRequest(JSONObject jsonObject) {
        this.deploymentUUID = jsonObject.getString("deploymentUUID");
        this.resultCode = jsonObject.getString("resultCode");
    }
}

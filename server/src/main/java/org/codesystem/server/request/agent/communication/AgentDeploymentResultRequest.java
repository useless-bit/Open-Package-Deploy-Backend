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
        if (!jsonObject.isNull("deploymentUUID")) {
            this.deploymentUUID = jsonObject.getString("deploymentUUID");
        } else {
            this.deploymentUUID = null;
        }
        if (!jsonObject.isNull("resultCode")) {
            this.resultCode = jsonObject.getString("resultCode");
        } else {
            this.resultCode = null;
        }
    }
}

package org.codesystem.payload;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateCheckResponseTest {
    UpdateCheckResponse updateCheckResponse;

    @BeforeEach
    void setup() {
        updateCheckResponse = null;
    }

    @Test
    void updateCheckResponse(){
        // null value
        updateCheckResponse = new UpdateCheckResponse(new JSONObject());
        Assertions.assertEquals(-1, updateCheckResponse.getUpdateInterval());
        Assertions.assertFalse(updateCheckResponse.isDeploymentAvailable());
        Assertions.assertNull(updateCheckResponse.getAgentChecksum());

        // invalid value
        JSONObject jsonObject = new JSONObject().put("updateInterval", "string").put("deploymentAvailable", "string").put("agentChecksum", JSONObject.NULL);
        updateCheckResponse = new UpdateCheckResponse(jsonObject);
        Assertions.assertEquals(-1, updateCheckResponse.getUpdateInterval());
        Assertions.assertFalse(updateCheckResponse.isDeploymentAvailable());
        Assertions.assertNull(updateCheckResponse.getAgentChecksum());

        // empty value
        jsonObject = new JSONObject().put("updateInterval", "").put("deploymentAvailable", "").put("agentChecksum", "");
        updateCheckResponse = new UpdateCheckResponse(jsonObject);
        Assertions.assertEquals(-1, updateCheckResponse.getUpdateInterval());
        Assertions.assertFalse(updateCheckResponse.isDeploymentAvailable());
        Assertions.assertNull(updateCheckResponse.getAgentChecksum());

        // blank value
        jsonObject = new JSONObject().put("updateInterval", "   ").put("deploymentAvailable", "   ").put("agentChecksum", "   ");
        updateCheckResponse = new UpdateCheckResponse(jsonObject);
        Assertions.assertEquals(-1, updateCheckResponse.getUpdateInterval());
        Assertions.assertFalse(updateCheckResponse.isDeploymentAvailable());
        Assertions.assertNull(updateCheckResponse.getAgentChecksum());

        // valid value
        jsonObject = new JSONObject().put("updateInterval",231241).put("deploymentAvailable", false).put("agentChecksum", "testChecksum");
        updateCheckResponse = new UpdateCheckResponse(jsonObject);
        Assertions.assertEquals(231241, updateCheckResponse.getUpdateInterval());
        Assertions.assertFalse(updateCheckResponse.isDeploymentAvailable());
        Assertions.assertEquals("testChecksum",updateCheckResponse.getAgentChecksum());
        jsonObject = new JSONObject().put("updateInterval", 231241).put("deploymentAvailable", true).put("agentChecksum", "testChecksum");
        updateCheckResponse = new UpdateCheckResponse(jsonObject);
        Assertions.assertEquals(231241, updateCheckResponse.getUpdateInterval());
        Assertions.assertTrue(updateCheckResponse.isDeploymentAvailable());
        Assertions.assertEquals("testChecksum",updateCheckResponse.getAgentChecksum());
    }
}
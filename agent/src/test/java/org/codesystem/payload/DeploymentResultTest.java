package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


class DeploymentResultTest {
    CryptoHandler cryptoHandler;
    DeploymentResult deploymentResult;

    @BeforeEach
    void setup() {
        deploymentResult = null;
        cryptoHandler = Mockito.mock(CryptoHandler.class);
        Mockito.when(cryptoHandler.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toJsonObject() {
        // null values
        deploymentResult = new DeploymentResult(null, null);
        Assertions.assertEquals(JSONObject.NULL, deploymentResult.toJsonObject(cryptoHandler).get("deploymentUUID"));
        Assertions.assertEquals(JSONObject.NULL, deploymentResult.toJsonObject(cryptoHandler).get("resultCode"));
        JSONObject jsonObject = deploymentResult.toJsonObject(null);
        Assertions.assertNull(jsonObject);

        // blank values
        deploymentResult = new DeploymentResult("", "");
        Assertions.assertEquals(JSONObject.NULL, deploymentResult.toJsonObject(cryptoHandler).get("deploymentUUID"));
        Assertions.assertEquals(JSONObject.NULL, deploymentResult.toJsonObject(cryptoHandler).get("resultCode"));
        deploymentResult = new DeploymentResult(" ", " ");
        Assertions.assertEquals(JSONObject.NULL, deploymentResult.toJsonObject(cryptoHandler).get("deploymentUUID"));
        Assertions.assertEquals(JSONObject.NULL, deploymentResult.toJsonObject(cryptoHandler).get("resultCode"));

        // valid trim
        deploymentResult = new DeploymentResult(" sampleUUID ", " sampleReturnCode ");
        jsonObject = deploymentResult.toJsonObject(cryptoHandler);
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
        Assertions.assertEquals("sampleUUID", jsonObject.getString("deploymentUUID"));
        Assertions.assertEquals("sampleReturnCode", jsonObject.getString("resultCode"));

        // valid
        deploymentResult = new DeploymentResult("sampleUUID", "sampleReturnCode");
        jsonObject = deploymentResult.toJsonObject(cryptoHandler);
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
        Assertions.assertEquals("sampleUUID", jsonObject.getString("deploymentUUID"));
        Assertions.assertEquals("sampleReturnCode", jsonObject.getString("resultCode"));

    }

}

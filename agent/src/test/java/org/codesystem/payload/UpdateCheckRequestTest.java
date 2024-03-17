package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.codesystem.HardwareInfo;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class UpdateCheckRequestTest {
    CryptoHandler cryptoHandler;
    UpdateCheckRequest updateCheckRequest;

    @BeforeEach
    void setup() {
        updateCheckRequest = new UpdateCheckRequest(null, null);
        cryptoHandler = Mockito.mock(CryptoHandler.class);
        Mockito.when(cryptoHandler.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toJsonObject() {
        // invalid
        updateCheckRequest = new UpdateCheckRequest(null, null);
        JSONObject jsonObject = updateCheckRequest.toJsonObject(null);
        Assertions.assertNull(jsonObject);

        // null values
        updateCheckRequest = new UpdateCheckRequest(null, null);
        jsonObject = updateCheckRequest.toJsonObject(cryptoHandler);
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("systemInformation"));
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
        updateCheckRequest = new UpdateCheckRequest("checksum", null);
        jsonObject = updateCheckRequest.toJsonObject(cryptoHandler);
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("systemInformation"));
        Assertions.assertEquals("checksum", jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
        updateCheckRequest = new UpdateCheckRequest(null, new DetailedSystemInformation(new HardwareInfo()));
        jsonObject = updateCheckRequest.toJsonObject(cryptoHandler);
        Assertions.assertNotNull(jsonObject.get("systemInformation"));
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));

        // valid
        updateCheckRequest = new UpdateCheckRequest("checksum", new DetailedSystemInformation(new HardwareInfo()));
        jsonObject = updateCheckRequest.toJsonObject(cryptoHandler);
        Assertions.assertNotNull(jsonObject.get("systemInformation"));
        Assertions.assertEquals("checksum", jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));

    }
}
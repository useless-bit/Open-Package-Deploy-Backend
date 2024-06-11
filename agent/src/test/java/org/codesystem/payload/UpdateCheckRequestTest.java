package org.codesystem.payload;

import org.codesystem.HardwareInfo;
import org.codesystem.utility.CryptoUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class UpdateCheckRequestTest {
    CryptoUtility cryptoUtility;
    UpdateCheckRequest updateCheckRequest;

    @BeforeEach
    void setUp() {
        updateCheckRequest = new UpdateCheckRequest(null, null);
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        Mockito.when(cryptoUtility.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toJsonObject() {
        // invalid
        updateCheckRequest = new UpdateCheckRequest(null, null);
        JSONObject jsonObject = updateCheckRequest.toJsonObject(null);
        Assertions.assertNull(jsonObject);

        // null values
        updateCheckRequest = new UpdateCheckRequest(null, null);
        jsonObject = updateCheckRequest.toJsonObject(cryptoUtility);
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("systemInformation"));
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
        updateCheckRequest = new UpdateCheckRequest("checksum", null);
        jsonObject = updateCheckRequest.toJsonObject(cryptoUtility);
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("systemInformation"));
        Assertions.assertEquals("checksum", jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
        updateCheckRequest = new UpdateCheckRequest(null, new DetailedSystemInformation(new HardwareInfo()));
        jsonObject = updateCheckRequest.toJsonObject(cryptoUtility);
        Assertions.assertNotNull(jsonObject.get("systemInformation"));
        Assertions.assertEquals(JSONObject.NULL, jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));

        // valid
        updateCheckRequest = new UpdateCheckRequest("checksum", new DetailedSystemInformation(new HardwareInfo()));
        jsonObject = updateCheckRequest.toJsonObject(cryptoUtility);
        Assertions.assertNotNull(jsonObject.get("systemInformation"));
        Assertions.assertEquals("checksum", jsonObject.get("agentChecksum"));
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));

    }
}
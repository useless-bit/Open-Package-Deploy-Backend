package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class EmptyRequestTest {
    CryptoHandler cryptoHandler;
    EmptyRequest emptyRequest;

    @BeforeEach
    void setup() {
        emptyRequest = new EmptyRequest();
        cryptoHandler = Mockito.mock(CryptoHandler.class);
        Mockito.when(cryptoHandler.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toJsonObject() {
        // invalid
        emptyRequest = new EmptyRequest();
        JSONObject jsonObject = emptyRequest.toJsonObject(null);
        Assertions.assertNull(jsonObject);

        // valid
        emptyRequest = new EmptyRequest();
        jsonObject = emptyRequest.toJsonObject(cryptoHandler);
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
    }
}
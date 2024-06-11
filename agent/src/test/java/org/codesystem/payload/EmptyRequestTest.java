package org.codesystem.payload;

import org.codesystem.utility.CryptoUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class EmptyRequestTest {
    CryptoUtility cryptoUtility;
    EmptyRequest emptyRequest;

    @BeforeEach
    void setUp() {
        emptyRequest = new EmptyRequest();
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        Mockito.when(cryptoUtility.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toJsonObject() {
        // invalid
        emptyRequest = new EmptyRequest();
        JSONObject jsonObject = emptyRequest.toJsonObject(null);
        Assertions.assertNull(jsonObject);

        // valid
        emptyRequest = new EmptyRequest();
        jsonObject = emptyRequest.toJsonObject(cryptoUtility);
        Assertions.assertNotNull(jsonObject.get("timestamp"));
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)), jsonObject.getString("signature"));
        Assertions.assertArrayEquals("Signature".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObject.getString("signature")));
    }
}
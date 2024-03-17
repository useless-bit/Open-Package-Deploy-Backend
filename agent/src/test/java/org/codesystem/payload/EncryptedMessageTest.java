package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.codesystem.PropertiesLoader;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class EncryptedMessageTest {
    EncryptedMessage encryptedMessage;
    PropertiesLoader propertiesLoader;
    CryptoHandler cryptoHandler;

    @BeforeEach
    void setup() {
        encryptedMessage = null;
        //Security.addProvider(new BouncyCastleProvider());
        propertiesLoader = Mockito.mock(PropertiesLoader.class);
        cryptoHandler = Mockito.mock(CryptoHandler.class);

    }

    @Test
    void toJsonObject() {
        // null values
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn(null);
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn(null);
        encryptedMessage = new EncryptedMessage(null, null, null);
        Assertions.assertNull(encryptedMessage.toJsonObject());
        encryptedMessage = new EncryptedMessage(null, cryptoHandler, propertiesLoader);
        Assertions.assertNull(encryptedMessage.toJsonObject());
        encryptedMessage = new EncryptedMessage(new JSONObject(), null, propertiesLoader);
        Assertions.assertNull(encryptedMessage.toJsonObject());
        encryptedMessage = new EncryptedMessage(new JSONObject(), cryptoHandler, null);
        Assertions.assertNull(encryptedMessage.toJsonObject());

        // empty values
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("");
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes(StandardCharsets.UTF_8));
        encryptedMessage = new EncryptedMessage(new JSONObject(), cryptoHandler, propertiesLoader);
        Assertions.assertNull(encryptedMessage.toJsonObject());
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("PublicKey");
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("".getBytes(StandardCharsets.UTF_8));
        encryptedMessage = new EncryptedMessage(new JSONObject(), cryptoHandler, propertiesLoader);
        Assertions.assertNull(encryptedMessage.toJsonObject());

        // blank values
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("   ");
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes(StandardCharsets.UTF_8));
        encryptedMessage = new EncryptedMessage(new JSONObject(), cryptoHandler, propertiesLoader);
        Assertions.assertNull(encryptedMessage.toJsonObject());

        // valid values
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("PublicKey");
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes(StandardCharsets.UTF_8));
        encryptedMessage = new EncryptedMessage(new JSONObject(), cryptoHandler, propertiesLoader);
        Assertions.assertEquals(new JSONObject().put("publicKeyBase64", "PublicKey").put("message", Base64.getEncoder().encodeToString("Encrypted".getBytes(StandardCharsets.UTF_8))).toString(), encryptedMessage.toJsonObject().toString());
        Assertions.assertArrayEquals("Encrypted".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(encryptedMessage.toJsonObject().getString("message")));
        Assertions.assertEquals("PublicKey", encryptedMessage.toJsonObject().getString("publicKeyBase64"));
    }
}
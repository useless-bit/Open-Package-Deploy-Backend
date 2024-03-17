package org.codesystem.payload;

import org.codesystem.AgentApplication;
import org.codesystem.CryptoHandler;
import org.codesystem.PropertiesLoader;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptedMessage {
    final String publicKeyBase64;
    final String message;


    public EncryptedMessage(JSONObject jsonObject, CryptoHandler cryptoHandler, PropertiesLoader propertiesLoader) {
        if (jsonObject == null || cryptoHandler == null || propertiesLoader == null) {
            publicKeyBase64 = null;
            message = null;
        } else {
            this.publicKeyBase64 = propertiesLoader.getProperty("Agent.ECC.Public-Key");
            this.message = Base64.getEncoder().encodeToString(cryptoHandler.encryptECC(jsonObject.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    public JSONObject toJsonObject() {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank() || message == null || message.isBlank()) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("publicKeyBase64", this.publicKeyBase64);
        jsonObject.put("message", this.message);
        return jsonObject;
    }
}

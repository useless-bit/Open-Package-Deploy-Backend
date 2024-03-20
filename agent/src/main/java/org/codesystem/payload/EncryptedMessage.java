package org.codesystem.payload;

import org.codesystem.PropertiesLoader;
import org.codesystem.utility.CryptoUtility;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptedMessage {
    final String publicKeyBase64;
    final String message;


    public EncryptedMessage(JSONObject jsonObject, CryptoUtility cryptoUtility, PropertiesLoader propertiesLoader) {
        if (jsonObject == null || cryptoUtility == null || propertiesLoader == null) {
            publicKeyBase64 = null;
            message = null;
        } else {
            this.publicKeyBase64 = propertiesLoader.getProperty("Agent.ECC.Public-Key");
            this.message = Base64.getEncoder().encodeToString(cryptoUtility.encryptECC(jsonObject.toString().getBytes(StandardCharsets.UTF_8)));
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

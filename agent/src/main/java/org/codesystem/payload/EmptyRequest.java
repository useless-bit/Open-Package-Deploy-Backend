package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Base64;

public class EmptyRequest {
    final Instant timestamp;

    public EmptyRequest() {
        this.timestamp = Instant.now();
    }

    public JSONObject toJsonObject(CryptoHandler cryptoHandler) {
        if (cryptoHandler == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        String signature = Base64.getEncoder().encodeToString(cryptoHandler.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

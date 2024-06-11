package org.codesystem.payload;

import org.codesystem.utility.CryptoUtility;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Base64;

public class EmptyRequest {
    final Instant timestamp;

    public EmptyRequest() {
        this.timestamp = Instant.now();
    }

    public JSONObject toJsonObject(CryptoUtility cryptoUtility) {
        if (cryptoUtility == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        String signature = Base64.getEncoder().encodeToString(cryptoUtility.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

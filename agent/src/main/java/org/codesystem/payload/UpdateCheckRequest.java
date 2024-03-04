package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Base64;

public class UpdateCheckRequest extends EmptyRequest {
    private final DetailedSystemInformation detailedSystemInformation;

    public UpdateCheckRequest() {
        super();
        this.detailedSystemInformation = new DetailedSystemInformation();
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        jsonObject.put("systemInformation", detailedSystemInformation.toJsonObject());
        CryptoHandler cryptoHandler = new CryptoHandler();
        String signature = Base64.getEncoder().encodeToString(cryptoHandler.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

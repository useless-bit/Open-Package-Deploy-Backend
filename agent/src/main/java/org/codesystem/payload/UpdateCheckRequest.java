package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.json.JSONObject;

import java.util.Base64;

public class UpdateCheckRequest extends EmptyRequest {
    private final DetailedSystemInformation detailedSystemInformation;
    private final String agentChecksum;

    public UpdateCheckRequest(String agentChecksum, DetailedSystemInformation detailedSystemInformation) {
        super();
        this.detailedSystemInformation = detailedSystemInformation;
        this.agentChecksum = agentChecksum;
    }

    @Override
    public JSONObject toJsonObject(CryptoHandler cryptoHandler) {
        if (cryptoHandler == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        if (detailedSystemInformation == null) {
            jsonObject.put("systemInformation", JSONObject.NULL);
        } else {
            jsonObject.put("systemInformation", detailedSystemInformation.toJsonObject());
        }
        if (agentChecksum == null) {
            jsonObject.put("agentChecksum", JSONObject.NULL);
        } else {
            jsonObject.put("agentChecksum", agentChecksum.trim());
        }
        String signature = Base64.getEncoder().encodeToString(cryptoHandler.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

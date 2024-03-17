package org.codesystem.payload;

import org.codesystem.CryptoHandler;
import org.json.JSONObject;

import java.util.Base64;

public class DeploymentResult extends EmptyRequest {
    private final String deploymentUUID;
    private final String resultCode;

    public DeploymentResult(String deploymentUUID, String resultCode) {
        this.deploymentUUID = deploymentUUID;
        this.resultCode = resultCode;
    }

    @Override
    public JSONObject toJsonObject(CryptoHandler cryptoHandler) {
        if (cryptoHandler == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        if (deploymentUUID == null || deploymentUUID.isBlank()) {
            jsonObject.put("deploymentUUID", JSONObject.NULL);
        } else {
            jsonObject.put("deploymentUUID", this.deploymentUUID.trim());
        }
        if (resultCode == null || resultCode.isBlank()) {
            jsonObject.put("resultCode", JSONObject.NULL);
        }else {
            jsonObject.put("resultCode", this.resultCode.trim());
        }
        String signature = Base64.getEncoder().encodeToString(cryptoHandler.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

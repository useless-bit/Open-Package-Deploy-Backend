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

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        jsonObject.put("deploymentUUID", this.deploymentUUID);
        jsonObject.put("resultCode", this.resultCode);
        CryptoHandler cryptoHandler = new CryptoHandler();
        String signature = Base64.getEncoder().encodeToString(cryptoHandler.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

package org.codesystem.payload;

import org.codesystem.AgentApplication;
import org.codesystem.CryptoHandler;
import org.codesystem.HardwareInfo;
import org.json.JSONObject;

import java.util.Base64;

public class UpdateCheckRequest extends EmptyRequest {
    private final DetailedSystemInformation detailedSystemInformation;
    private final String agentChecksum;
    private final CryptoHandler cryptoHandler;

    public UpdateCheckRequest(CryptoHandler cryptoHandler) {
        super();
        this.cryptoHandler = cryptoHandler;
        this.detailedSystemInformation = new DetailedSystemInformation(new HardwareInfo());
        this.agentChecksum = AgentApplication.agentChecksum;
    }

    @Override
    public JSONObject toJsonObject(CryptoHandler cryptoHandler) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        jsonObject.put("systemInformation", detailedSystemInformation.toJsonObject());
        jsonObject.put("agentChecksum", this.agentChecksum);
        String signature = Base64.getEncoder().encodeToString(cryptoHandler.createSignatureECC(jsonObject.toString()));
        jsonObject.put("signature", signature);
        return jsonObject;
    }
}

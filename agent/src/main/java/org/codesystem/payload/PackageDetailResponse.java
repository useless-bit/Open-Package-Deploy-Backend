package org.codesystem.payload;

import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PackageDetailResponse {
    private final String deploymentUUID;
    private final SecretKey encryptionToken;
    private final byte[] initializationVector;
    private final String checksumPlaintext;
    private final String checksumEncrypted;

    public PackageDetailResponse(JSONObject jsonObject) {
        this.deploymentUUID = jsonObject.getString("deploymentUUID");
        this.encryptionToken = new SecretKeySpec(Base64.getDecoder().decode(jsonObject.getString("encryptionToken")), "AES");
        this.initializationVector = Base64.getDecoder().decode(jsonObject.getString("initializationVector"));
        this.checksumPlaintext = jsonObject.getString("checksumPlaintext");
        this.checksumEncrypted = jsonObject.getString("checksumEncrypted");
    }

    public String getDeploymentUUID() {
        return deploymentUUID;
    }

    public SecretKey getEncryptionToken() {
        return encryptionToken;
    }

    public byte[] getInitializationVector() {
        return initializationVector;
    }

    public String getChecksumPlaintext() {
        return checksumPlaintext;
    }

    public String getChecksumEncrypted() {
        return checksumEncrypted;
    }
}
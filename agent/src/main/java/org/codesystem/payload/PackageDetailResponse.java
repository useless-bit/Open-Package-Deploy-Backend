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
        if (jsonObject.isNull("deploymentUUID") || jsonObject.getString("deploymentUUID").isBlank()) {
            this.deploymentUUID = null;
        } else {
            this.deploymentUUID = jsonObject.getString("deploymentUUID").trim();
        }
        if (jsonObject.isNull("encryptionToken") || jsonObject.getString("encryptionToken").isBlank()) {
            this.encryptionToken = null;
        } else {
            this.encryptionToken = new SecretKeySpec(Base64.getDecoder().decode(jsonObject.getString("encryptionToken")), "AES");
        }
        if (jsonObject.isNull("initializationVector") || jsonObject.getString("initializationVector").isBlank()) {
            this.initializationVector = null;
        } else {
            this.initializationVector = Base64.getDecoder().decode(jsonObject.getString("initializationVector"));
        }
        if (jsonObject.isNull("checksumPlaintext") || jsonObject.getString("checksumPlaintext").isBlank()) {
            this.checksumPlaintext = null;
        } else {
            this.checksumPlaintext = jsonObject.getString("checksumPlaintext").trim();
        }
        if (jsonObject.isNull("checksumEncrypted") || jsonObject.getString("checksumEncrypted").isBlank()) {
            this.checksumEncrypted = null;
        } else {
            this.checksumEncrypted = jsonObject.getString("checksumEncrypted").trim();
        }
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
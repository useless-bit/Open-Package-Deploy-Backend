package org.codesystem.payload;

import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PackageDetailResponse {
    private static final String DEPLOYMENT_UUID = "deploymentUUID";
    private static final String ENCRYPTION_TOKEN = "encryptionToken";
    private static final String INITIALIZATION_VECTOR = "initializationVector";
    private static final String CHECKSUM_PLAINTEXT = "checksumPlaintext";
    private static final String CHECKSUM_ENCRYPTED = "checksumEncrypted";
    private final String deploymentUUID;
    private final SecretKey encryptionToken;
    private final byte[] initializationVector;
    private final String checksumPlaintext;
    private final String checksumEncrypted;

    public PackageDetailResponse(JSONObject jsonObject) {
        if (jsonObject.isNull(DEPLOYMENT_UUID) || jsonObject.getString(DEPLOYMENT_UUID).isBlank()) {
            this.deploymentUUID = null;
        } else {
            this.deploymentUUID = jsonObject.getString(DEPLOYMENT_UUID).trim();
        }
        if (jsonObject.isNull(ENCRYPTION_TOKEN) || jsonObject.getString(ENCRYPTION_TOKEN).isBlank()) {
            this.encryptionToken = null;
        } else {
            this.encryptionToken = new SecretKeySpec(Base64.getDecoder().decode(jsonObject.getString(ENCRYPTION_TOKEN)), "AES");
        }
        if (jsonObject.isNull(INITIALIZATION_VECTOR) || jsonObject.getString(INITIALIZATION_VECTOR).isBlank()) {
            this.initializationVector = null;
        } else {
            this.initializationVector = Base64.getDecoder().decode(jsonObject.getString(INITIALIZATION_VECTOR));
        }
        if (jsonObject.isNull(CHECKSUM_PLAINTEXT) || jsonObject.getString(CHECKSUM_PLAINTEXT).isBlank()) {
            this.checksumPlaintext = null;
        } else {
            this.checksumPlaintext = jsonObject.getString(CHECKSUM_PLAINTEXT).trim();
        }
        if (jsonObject.isNull(CHECKSUM_ENCRYPTED) || jsonObject.getString(CHECKSUM_ENCRYPTED).isBlank()) {
            this.checksumEncrypted = null;
        } else {
            this.checksumEncrypted = jsonObject.getString(CHECKSUM_ENCRYPTED).trim();
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
package org.codesystem.server.response.agent.communication;

import org.codesystem.server.entity.DeploymentEntity;
import org.json.JSONObject;

import java.util.Base64;

public class AgentPackageDetailResponse {
    private final String deploymentUUID;
    private final String encryptionToken;
    private final String initializationVector;
    private final String checksumPlaintext;
    private final String checksumEncrypted;

    public AgentPackageDetailResponse(DeploymentEntity deploymentEntity) {
        this.deploymentUUID = deploymentEntity.getUuid();
        this.encryptionToken = Base64.getEncoder().encodeToString(deploymentEntity.getPackageEntity().getEncryptionToken().getEncoded());
        this.initializationVector = Base64.getEncoder().encodeToString(deploymentEntity.getPackageEntity().getInitializationVector());
        this.checksumPlaintext = deploymentEntity.getPackageEntity().getChecksumPlaintext();
        this.checksumEncrypted = deploymentEntity.getPackageEntity().getChecksumEncrypted();
    }

    public JSONObject toJsonObject() {
        return new JSONObject()
                .put("deploymentUUID", this.deploymentUUID)
                .put("encryptionToken", this.encryptionToken)
                .put("initializationVector", this.initializationVector)
                .put("checksumPlaintext", this.checksumPlaintext)
                .put("checksumEncrypted", this.checksumEncrypted);
    }
}

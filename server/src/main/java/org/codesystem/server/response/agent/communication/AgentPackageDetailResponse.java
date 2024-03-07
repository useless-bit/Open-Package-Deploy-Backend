package org.codesystem.server.response.agent.communication;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.DeploymentEntity;
import org.json.JSONObject;

import java.util.Base64;

@Getter
@Setter
public class AgentPackageDetailResponse {
    private String deploymentUUID;
    private String encryptionToken;
    private String initializationVector;
    private String checksumPlaintext;
    private String checksumEncrypted;

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

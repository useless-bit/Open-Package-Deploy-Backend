package org.codesystem.enums;

public enum PackageDeploymentErrorState {
    UNKNOWN_ERROR,
    DECRYPTION_FAILED,
    PLAINTEXT_CHECKSUM_MISMATCH,
    ENCRYPTED_CHECKSUM_MISMATCH,
    ENTRYPOINT_NOT_FOUND;

    @Override
    public String toString() {
        return "AGENT-DEPLOYMENT-ERROR: " + super.toString();
    }
}

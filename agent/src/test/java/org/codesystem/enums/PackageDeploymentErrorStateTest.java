package org.codesystem.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PackageDeploymentErrorStateTest {

    @Test
    void testToString() {
        Assertions.assertEquals("AGENT-DEPLOYMENT-ERROR: DECRYPTION_FAILED", PackageDeploymentErrorState.DECRYPTION_FAILED.toString());
        Assertions.assertEquals("AGENT-DEPLOYMENT-ERROR: PLAINTEXT_CHECKSUM_MISMATCH", PackageDeploymentErrorState.PLAINTEXT_CHECKSUM_MISMATCH.toString());
        Assertions.assertEquals("AGENT-DEPLOYMENT-ERROR: ENCRYPTED_CHECKSUM_MISMATCH", PackageDeploymentErrorState.ENCRYPTED_CHECKSUM_MISMATCH.toString());
        Assertions.assertEquals("AGENT-DEPLOYMENT-ERROR: ENTRYPOINT_NOT_FOUND", PackageDeploymentErrorState.ENTRYPOINT_NOT_FOUND.toString());
    }

}
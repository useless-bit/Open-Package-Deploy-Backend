package org.codesystem.payload;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class PackageDetailResponseTest {
    PackageDetailResponse packageDetailResponse;

    @BeforeEach
    void setup() {
        packageDetailResponse = null;
    }

    @Test
    void packageDetailResponse() {
        // null value
        packageDetailResponse = new PackageDetailResponse(new JSONObject());
        Assertions.assertNull(packageDetailResponse.getDeploymentUUID());
        Assertions.assertNull(packageDetailResponse.getEncryptionToken());
        Assertions.assertNull(packageDetailResponse.getInitializationVector());
        Assertions.assertNull(packageDetailResponse.getChecksumPlaintext());
        Assertions.assertNull(packageDetailResponse.getChecksumEncrypted());

        // empty value
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", "").put("encryptionToken", "").put("initializationVector", "").put("checksumPlaintext", "").put("checksumEncrypted", "");
        packageDetailResponse = new PackageDetailResponse(jsonObject);
        Assertions.assertNull(packageDetailResponse.getDeploymentUUID());
        Assertions.assertNull(packageDetailResponse.getEncryptionToken());
        Assertions.assertNull(packageDetailResponse.getInitializationVector());
        Assertions.assertNull(packageDetailResponse.getChecksumPlaintext());
        Assertions.assertNull(packageDetailResponse.getChecksumEncrypted());

        // blank value
        jsonObject = new JSONObject().put("deploymentUUID", "   ").put("encryptionToken", "   ").put("initializationVector", "   ").put("checksumPlaintext", "   ").put("checksumEncrypted", "   ");
        packageDetailResponse = new PackageDetailResponse(jsonObject);
        Assertions.assertNull(packageDetailResponse.getDeploymentUUID());
        Assertions.assertNull(packageDetailResponse.getEncryptionToken());
        Assertions.assertNull(packageDetailResponse.getInitializationVector());
        Assertions.assertNull(packageDetailResponse.getChecksumPlaintext());
        Assertions.assertNull(packageDetailResponse.getChecksumEncrypted());

        // valid value
        jsonObject = new JSONObject().put("deploymentUUID", " sample UUID ").put("encryptionToken", Base64.getEncoder().encodeToString("encryptionToken".getBytes(StandardCharsets.UTF_8))).put("initializationVector", Base64.getEncoder().encodeToString("init Vector".getBytes(StandardCharsets.UTF_8))).put("checksumPlaintext", " plaintext checksum ").put("checksumEncrypted", " encrypted checksum ");
        packageDetailResponse = new PackageDetailResponse(jsonObject);
        Assertions.assertEquals("sample UUID", packageDetailResponse.getDeploymentUUID());
        Assertions.assertArrayEquals(new SecretKeySpec("encryptionToken".getBytes(StandardCharsets.UTF_8), "AES").getEncoded(), packageDetailResponse.getEncryptionToken().getEncoded());
        Assertions.assertArrayEquals("init Vector".getBytes(StandardCharsets.UTF_8), packageDetailResponse.getInitializationVector());
        Assertions.assertEquals("plaintext checksum", packageDetailResponse.getChecksumPlaintext());
        Assertions.assertEquals("encrypted checksum", packageDetailResponse.getChecksumEncrypted());
    }

}
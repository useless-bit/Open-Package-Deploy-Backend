package org.codesystem.server.response.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@Setter
@AllArgsConstructor
public class GetPackageResponse implements ApiResponse {
    private String uuid;
    private String name;
    private String expectedReturnValue;
    private PackageStatusInternal packageStatusInternal;
    private String checksumPlaintext;
    private String checksumEncrypted;
    private OperatingSystem targetOperatingSystem;
    private Double plaintextSize;
    private Double encryptedSize;

    public GetPackageResponse(PackageEntity packageEntity) {
        this.uuid = packageEntity.getUuid();
        this.name = packageEntity.getName();
        this.expectedReturnValue = packageEntity.getExpectedReturnValue();
        this.packageStatusInternal = packageEntity.getPackageStatusInternal();
        this.checksumPlaintext = packageEntity.getChecksumPlaintext();
        this.checksumEncrypted = packageEntity.getChecksumEncrypted();
        this.targetOperatingSystem = packageEntity.getTargetOperatingSystem();
        this.plaintextSize = packageEntity.getPlaintextSize();
        this.encryptedSize = packageEntity.getEncryptedSize();
    }
}

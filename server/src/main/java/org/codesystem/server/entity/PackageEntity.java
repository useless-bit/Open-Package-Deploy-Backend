package org.codesystem.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codesystem.server.converter.OperatingSystemConverter;
import org.codesystem.server.converter.PackageStatusInternalConverter;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "package")
public class PackageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @Column(name = "name", nullable = false)
    private String name = null;

    @Column(name = "expected_return_value")
    private String expectedReturnValue;

    @Column(name = "encryption_token")
    @JsonIgnore
    private SecretKey encryptionToken = null;

    @Column(name = "package_status_internal")
    @Convert(converter = PackageStatusInternalConverter.class)
    private PackageStatusInternal packageStatusInternal;

    @Column(name = "checksum_plaintext", nullable = false)
    private String checksumPlaintext;

    @Column(name = "checksum_encrypted")
    private String checksumEncrypted;

    @Column(name = "initialization_vector")
    @JsonIgnore
    private byte[] initializationVector;

    @Column(name = "target_operating_system", nullable = false)
    @Convert(converter = OperatingSystemConverter.class)
    private OperatingSystem targetOperatingSystem;

    @Column(name = "plaintext_size", nullable = false)
    private Long plaintextSize = 0L;

    @Column(name = "encrypted_size", nullable = false)
    private Long encryptedSize = 0L;

    @ManyToMany(mappedBy = "deployedPackages")
    List<GroupEntity> groups = new ArrayList<>();
}

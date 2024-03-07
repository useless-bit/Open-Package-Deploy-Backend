package org.codesystem.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "server")
public class ServerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @Column(name = "public_key", nullable = false, length = 2024)
    private String publicKeyBase64 = null;

    @Column(name = "private_key", nullable = false, length = 2024)
    private String privateKeyBase64 = null;

    @Column(name = "agent_registration_token", nullable = false)
    private String agentRegistrationToken = null;

    @Column(name = "agent_checksum", nullable = false)
    private String agentChecksum = null;
}

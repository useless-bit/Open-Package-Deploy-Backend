package org.codesystem.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codesystem.server.converter.OperatingSystemConverter;
import org.codesystem.server.enums.agent.OperatingSystem;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "agent")
public class AgentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "public_key", nullable = false, length = 2024)
    @JsonIgnore()
    private String publicKeyBase64 = null;

    @Column(name = "validation_token")
    @JsonIgnore
    private String validationToken = null;

    @Column(name = "registration_completed")
    private boolean registrationCompleted = false;

    @Column(name = "last_connection_time")
    private Instant lastConnectionTime = null;

    @Column(name = "agent_checksum")
    private String agentChecksum;

    @Column(name = "operating_system", nullable = false)
    @Convert(converter = OperatingSystemConverter.class)
    private OperatingSystem operatingSystem = OperatingSystem.UNKNOWN;

    @Column(name = "operating_system_family")
    private String operatingSystemFamily = null;

    @Column(name = "operating_system_architecture")
    private String operatingSystemArchitecture = null;

    @Column(name = "operating_system_version")
    private String operatingSystemVersion = null;

    @Column(name = "operating_system_code_name")
    private String operatingSystemCodeName = null;

    @Column(name = "operating_system_build_number")
    private String operatingSystemBuildNumber = null;

    @Column(name = "cpu_name")
    private String CpuName = null;

    @Column(name = "cpu_architecture")
    private String CpuArchitecture = null;

    @Column(name = "cpu_logical_cores")
    private String CpuLogicalCores = null;

    @Column(name = "cpu_physical_cores")
    private String CpuPhysicalCores = null;

    @Column(name = "cpu_sockets")
    private String CpuSockets = null;

    @Column(name = "memory")
    private String Memory = null;
}

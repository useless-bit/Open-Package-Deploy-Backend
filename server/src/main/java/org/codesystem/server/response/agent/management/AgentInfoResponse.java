package org.codesystem.server.response.agent.management;

import lombok.Getter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.response.general.ApiResponse;

import java.time.Instant;

@Getter
public class AgentInfoResponse implements ApiResponse {
    private final String uuid;
    private final String name;
    private final boolean registrationCompleted;
    private final Instant lastConnectionTime;
    private final String checksum;
    private final OperatingSystem operatingSystem;
    private final String operatingSystemFamily;
    private final String operatingSystemArchitecture;
    private final String operatingSystemVersion;
    private final String operatingSystemCodeName;
    private final String operatingSystemBuildNumber;
    private final String cpuName;
    private final String cpuArchitecture;
    private final String cpuLogicalCores;
    private final String cpuPhysicalCores;
    private final String cpuSockets;
    private final String memory;

    public AgentInfoResponse(AgentEntity agent) {
        this.uuid = agent.getUuid();
        this.name = agent.getName();
        this.registrationCompleted = agent.isRegistrationCompleted();
        this.lastConnectionTime = agent.getLastConnectionTime();
        this.checksum = agent.getAgentChecksum();
        this.operatingSystem = agent.getOperatingSystem();
        this.operatingSystemFamily = agent.getOperatingSystemFamily();
        this.operatingSystemArchitecture = agent.getOperatingSystemArchitecture();
        this.operatingSystemVersion = agent.getOperatingSystemVersion();
        this.operatingSystemCodeName = agent.getOperatingSystemCodeName();
        this.operatingSystemBuildNumber = agent.getOperatingSystemBuildNumber();
        this.cpuName = agent.getCpuName();
        this.cpuArchitecture = agent.getCpuArchitecture();
        this.cpuLogicalCores = agent.getCpuLogicalCores();
        this.cpuPhysicalCores = agent.getCpuPhysicalCores();
        this.cpuSockets = agent.getCpuSockets();
        this.memory = agent.getMemory();
    }
}
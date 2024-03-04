package org.codesystem.server.response.agent.management;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.response.general.ApiResponse;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class GetAgentResponse implements ApiResponse {
    private String uuid;
    private String name;
    private boolean registrationCompleted;
    private Instant lastConnectionTime;
    private OperatingSystem operatingSystem;
    private String operatingSystemFamily;
    private String operatingSystemArchitecture;
    private String operatingSystemVersion;
    private String operatingSystemCodeName;
    private String operatingSystemBuildNumber;
    private String cpuName;
    private String cpuArchitecture;
    private String cpuLogicalCores;
    private String cpuPhysicalCores;
    private String cpuSockets;
    private String memory;

    public GetAgentResponse(AgentEntity agent) {
        this.uuid = agent.getUuid();
        this.name = agent.getName();
        this.registrationCompleted = agent.isRegistrationCompleted();
        this.lastConnectionTime = agent.getLastConnectionTime();
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
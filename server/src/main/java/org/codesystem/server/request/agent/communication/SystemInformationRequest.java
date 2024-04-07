package org.codesystem.server.request.agent.communication;

import lombok.Getter;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.json.JSONObject;

@Getter
public class SystemInformationRequest {
    private final String operatingSystemFamily;
    private final String operatingSystemArchitecture;
    private final String operatingSystemVersion;
    private final String operatingSystemCodeName;
    private final String operatingSystemBuildNumber;
    private final String cpuName;
    private final String cpuArchitecture;
    private final String cpuSockets;
    private final String cpuLogicalCores;
    private final String cpuPhysicalCores;
    private final String memory;
    private OperatingSystem operatingSystem;

    public SystemInformationRequest(JSONObject jsonObject) {
        try {
            this.operatingSystem = OperatingSystem.valueOf(jsonObject.getString("operatingSystem").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.operatingSystem = OperatingSystem.UNKNOWN;
        }
        this.operatingSystemFamily = jsonObject.optString("operatingSystemFamily");
        this.operatingSystemArchitecture = jsonObject.optString("operatingSystemArchitecture");
        this.operatingSystemVersion = jsonObject.optString("operatingSystemVersion");
        this.operatingSystemCodeName = jsonObject.optString("operatingSystemCodeName");
        this.operatingSystemBuildNumber = jsonObject.optString("operatingSystemBuildNumber");
        this.cpuName = jsonObject.optString("cpuName");
        this.cpuArchitecture = jsonObject.optString("cpuArchitecture");
        this.cpuLogicalCores = jsonObject.optString("cpuLogicalCores");
        this.cpuPhysicalCores = jsonObject.optString("cpuPhysicalCores");
        this.cpuSockets = jsonObject.optString("cpuSockets");
        this.memory = jsonObject.optString("memory");
    }
}

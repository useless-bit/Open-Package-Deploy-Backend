package org.codesystem.server.request.agent.communication;

import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.json.JSONObject;

@Getter
@Setter
public class SystemInformationRequest {
    private OperatingSystem operatingSystem;
    private String operatingSystemFamily;
    private String operatingSystemArchitecture;
    private String operatingSystemVersion;
    private String operatingSystemCodeName;
    private String operatingSystemBuildNumber;
    private String cpuName;
    private String cpuArchitecture;
    private String cpuSockets;
    private String cpuLogicalCores;
    private String cpuPhysicalCores;
    private String memory;

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

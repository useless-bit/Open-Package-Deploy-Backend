package org.codesystem.payload;

import org.codesystem.HardwareInfo;
import org.json.JSONObject;

public class DetailedSystemInformation {
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
    private String operatingSystem;

    public DetailedSystemInformation() {
        HardwareInfo hardwareInfo = new HardwareInfo();

        // load OS data
        if (hardwareInfo.getOsManufacturer().toLowerCase().contains("linux")) {
            this.operatingSystem = "Linux";
        }
        this.operatingSystemFamily = hardwareInfo.getOsFamily();
        this.operatingSystemArchitecture = hardwareInfo.getOsArchitecture() + "-Bit";
        this.operatingSystemVersion = hardwareInfo.getOsVersion();
        this.operatingSystemCodeName = hardwareInfo.getOsCodeName();
        this.operatingSystemBuildNumber = hardwareInfo.getOsBuildNumber();

        // load hardware data
        this.cpuName = hardwareInfo.getHwCpuName();
        this.cpuArchitecture = hardwareInfo.getHwCpuArchitecture();
        this.cpuLogicalCores = hardwareInfo.getHwCpuLogicalCoreCount();
        this.cpuPhysicalCores = hardwareInfo.getHwCpuPhysicalCoreCount();
        this.cpuSockets = hardwareInfo.getHwCpuSocketCount();
        this.memory = hardwareInfo.getHwMemory();
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operatingSystem", this.operatingSystem);
        jsonObject.put("operatingSystemFamily", this.operatingSystemFamily);
        jsonObject.put("operatingSystemArchitecture", this.operatingSystemArchitecture);
        jsonObject.put("operatingSystemVersion", this.operatingSystemVersion);
        jsonObject.put("operatingSystemCodeName", this.operatingSystemCodeName);
        jsonObject.put("operatingSystemBuildNumber", this.operatingSystemBuildNumber);
        jsonObject.put("cpuName", this.cpuName);
        jsonObject.put("cpuArchitecture", this.cpuArchitecture);
        jsonObject.put("cpuLogicalCores", this.cpuLogicalCores);
        jsonObject.put("cpuPhysicalCores", this.cpuPhysicalCores);
        jsonObject.put("cpuSockets", this.cpuSockets);
        jsonObject.put("memory", this.memory);
        return jsonObject;
    }
}

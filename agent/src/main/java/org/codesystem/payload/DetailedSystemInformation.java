package org.codesystem.payload;

import org.codesystem.HardwareInfo;
import org.codesystem.enums.OperatingSystem;
import org.json.JSONObject;

import java.util.Objects;

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
    private final OperatingSystem operatingSystem;

    public DetailedSystemInformation(HardwareInfo hardwareInfo) {
        // load OS data
        if (hardwareInfo.getOsManufacturer() == null) {
            this.operatingSystem = null;
        } else if (hardwareInfo.getOsManufacturer().toLowerCase().contains("linux")) {
            this.operatingSystem = OperatingSystem.LINUX;
        } else if (hardwareInfo.getOsManufacturer().toLowerCase().contains("microsoft")) {
            this.operatingSystem = OperatingSystem.WINDOWS;
        } else if (hardwareInfo.getOsManufacturer().toLowerCase().contains("apple")) {
            this.operatingSystem = OperatingSystem.MACOS;
        } else {
            this.operatingSystem = null;
        }
        this.operatingSystemFamily = hardwareInfo.getOsFamily();
        this.operatingSystemArchitecture = hardwareInfo.getOsArchitecture();
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
        jsonObject.put("operatingSystem", Objects.requireNonNullElse(this.operatingSystem, JSONObject.NULL));
        if (this.operatingSystemFamily == null || this.operatingSystemFamily.isBlank()) {
            jsonObject.put("operatingSystemFamily", JSONObject.NULL);
        } else {
            jsonObject.put("operatingSystemFamily", this.operatingSystemFamily.trim());
        }
        if (this.operatingSystemArchitecture == null || this.operatingSystemArchitecture.isBlank()) {
            jsonObject.put("operatingSystemArchitecture", JSONObject.NULL);
        } else {
            jsonObject.put("operatingSystemArchitecture", this.operatingSystemArchitecture.trim() + "-Bit");
        }
        if (this.operatingSystemVersion == null || this.operatingSystemVersion.isBlank()) {
            jsonObject.put("operatingSystemVersion", JSONObject.NULL);
        } else {
            jsonObject.put("operatingSystemVersion", this.operatingSystemVersion.trim());
        }
        if (this.operatingSystemCodeName == null || this.operatingSystemCodeName.isBlank()) {
            jsonObject.put("operatingSystemCodeName", JSONObject.NULL);
        } else {
            jsonObject.put("operatingSystemCodeName", this.operatingSystemCodeName.trim());
        }
        if (this.operatingSystemBuildNumber == null || this.operatingSystemBuildNumber.isBlank()) {
            jsonObject.put("operatingSystemBuildNumber", JSONObject.NULL);
        } else {
            jsonObject.put("operatingSystemBuildNumber", this.operatingSystemBuildNumber.trim());
        }
        if (this.cpuName == null || this.cpuName.isBlank()) {
            jsonObject.put("cpuName", JSONObject.NULL);
        } else {
            jsonObject.put("cpuName", this.cpuName.trim());
        }
        if (this.cpuArchitecture == null || this.cpuArchitecture.isBlank()) {
            jsonObject.put("cpuArchitecture", JSONObject.NULL);
        } else {
            jsonObject.put("cpuArchitecture", this.cpuArchitecture.trim());
        }
        if (this.cpuLogicalCores == null || this.cpuLogicalCores.isBlank()) {
            jsonObject.put("cpuLogicalCores", JSONObject.NULL);
        } else {
            jsonObject.put("cpuLogicalCores", this.cpuLogicalCores.trim());
        }
        if (this.cpuPhysicalCores == null || this.cpuPhysicalCores.isBlank()) {
            jsonObject.put("cpuPhysicalCores", JSONObject.NULL);
        } else {
            jsonObject.put("cpuPhysicalCores", this.cpuPhysicalCores.trim());
        }
        if (this.cpuSockets == null || this.cpuSockets.isBlank()) {
            jsonObject.put("cpuSockets", JSONObject.NULL);
        } else {
            jsonObject.put("cpuSockets", this.cpuSockets.trim());
        }
        if (this.memory == null || this.memory.isBlank()) {
            jsonObject.put("memory", JSONObject.NULL);
        } else {
            jsonObject.put("memory", this.memory.trim());
        }
        return jsonObject;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }
}

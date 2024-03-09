package org.codesystem;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

public class HardwareInfo {
    private final HardwareAbstractionLayer hardwareAbstractionLayer;
    private final OperatingSystem operatingSystem;

    public HardwareInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.hardwareAbstractionLayer = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    public String getOsManufacturer() {
        return operatingSystem.getManufacturer();
    }

    public String getOsFamily() {
        return operatingSystem.getFamily();
    }

    public String getOsArchitecture() {
        return Integer.toString(operatingSystem.getBitness());
    }

    public String getOsVersion() {
        return operatingSystem.getVersionInfo().getVersion();
    }

    public String getOsCodeName() {
        return operatingSystem.getVersionInfo().getCodeName();
    }

    public String getOsBuildNumber() {
        return operatingSystem.getVersionInfo().getBuildNumber();
    }

    public String getHwCpuName() {
        return hardwareAbstractionLayer.getProcessor().getProcessorIdentifier().getName();
    }

    public String getHwCpuArchitecture() {
        return hardwareAbstractionLayer.getProcessor().getProcessorIdentifier().getMicroarchitecture();
    }

    public String getHwCpuLogicalCoreCount() {
        return String.valueOf(hardwareAbstractionLayer.getProcessor().getLogicalProcessorCount());
    }

    public String getHwCpuPhysicalCoreCount() {
        return String.valueOf(hardwareAbstractionLayer.getProcessor().getPhysicalProcessorCount());
    }

    public String getHwCpuSocketCount() {
        return String.valueOf(hardwareAbstractionLayer.getProcessor().getPhysicalPackageCount());
    }

    public String getHwMemory() {
        return String.valueOf(hardwareAbstractionLayer.getMemory().getTotal());
    }

    public boolean isElevated() {
        return operatingSystem.isElevated();
    }

}
package org.codesystem.payload;

import org.codesystem.HardwareInfo;
import org.codesystem.enums.OperatingSystem;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DetailedSystemInformationTest {
    HardwareInfo hardwareInfo;
    DetailedSystemInformation detailedSystemInformation;

    @BeforeEach
    void setup() {
        detailedSystemInformation = null;
        hardwareInfo = Mockito.mock(HardwareInfo.class);
    }

    @Test
    void toJsonObject() {
        // null values
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn(null);
        Mockito.when(hardwareInfo.getOsFamily()).thenReturn(null);
        Mockito.when(hardwareInfo.getOsArchitecture()).thenReturn(null);
        Mockito.when(hardwareInfo.getOsVersion()).thenReturn(null);
        Mockito.when(hardwareInfo.getOsCodeName()).thenReturn(null);
        Mockito.when(hardwareInfo.getHwCpuName()).thenReturn(null);
        Mockito.when(hardwareInfo.getHwCpuArchitecture()).thenReturn(null);
        Mockito.when(hardwareInfo.getHwCpuLogicalCoreCount()).thenReturn(null);
        Mockito.when(hardwareInfo.getHwCpuPhysicalCoreCount()).thenReturn(null);
        Mockito.when(hardwareInfo.getHwCpuSocketCount()).thenReturn(null);
        Mockito.when(hardwareInfo.getHwMemory()).thenReturn(null);
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemFamily"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemArchitecture"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemVersion"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemCodeName"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemBuildNumber"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuName"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuArchitecture"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuLogicalCores"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuPhysicalCores"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuSockets"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("memory"));

        // empty values
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("");
        Mockito.when(hardwareInfo.getOsFamily()).thenReturn("");
        Mockito.when(hardwareInfo.getOsArchitecture()).thenReturn("");
        Mockito.when(hardwareInfo.getOsVersion()).thenReturn("");
        Mockito.when(hardwareInfo.getOsCodeName()).thenReturn("");
        Mockito.when(hardwareInfo.getHwCpuName()).thenReturn("");
        Mockito.when(hardwareInfo.getHwCpuArchitecture()).thenReturn("");
        Mockito.when(hardwareInfo.getHwCpuLogicalCoreCount()).thenReturn("");
        Mockito.when(hardwareInfo.getHwCpuPhysicalCoreCount()).thenReturn("");
        Mockito.when(hardwareInfo.getHwCpuSocketCount()).thenReturn("");
        Mockito.when(hardwareInfo.getHwMemory()).thenReturn("");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemFamily"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemArchitecture"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemVersion"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemCodeName"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemBuildNumber"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuName"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuArchitecture"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuLogicalCores"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuPhysicalCores"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuSockets"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("memory"));

        // blank values
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("   ");
        Mockito.when(hardwareInfo.getOsFamily()).thenReturn("   ");
        Mockito.when(hardwareInfo.getOsArchitecture()).thenReturn("   ");
        Mockito.when(hardwareInfo.getOsVersion()).thenReturn("   ");
        Mockito.when(hardwareInfo.getOsCodeName()).thenReturn("   ");
        Mockito.when(hardwareInfo.getHwCpuName()).thenReturn("   ");
        Mockito.when(hardwareInfo.getHwCpuArchitecture()).thenReturn("   ");
        Mockito.when(hardwareInfo.getHwCpuLogicalCoreCount()).thenReturn("   ");
        Mockito.when(hardwareInfo.getHwCpuPhysicalCoreCount()).thenReturn("   ");
        Mockito.when(hardwareInfo.getHwCpuSocketCount()).thenReturn("   ");
        Mockito.when(hardwareInfo.getHwMemory()).thenReturn("   ");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemFamily"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemArchitecture"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemVersion"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemCodeName"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("operatingSystemBuildNumber"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuName"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuArchitecture"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuLogicalCores"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuPhysicalCores"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("cpuSockets"));
        Assertions.assertEquals(JSONObject.NULL, detailedSystemInformation.toJsonObject().get("memory"));

        // valid values trim
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("linux");
        Mockito.when(hardwareInfo.getOsFamily()).thenReturn(" OsFamily ");
        Mockito.when(hardwareInfo.getOsArchitecture()).thenReturn(" OsArch ");
        Mockito.when(hardwareInfo.getOsVersion()).thenReturn(" OsVersion ");
        Mockito.when(hardwareInfo.getOsCodeName()).thenReturn(" OsCodeName ");
        Mockito.when(hardwareInfo.getOsBuildNumber()).thenReturn(" OsBuild ");
        Mockito.when(hardwareInfo.getHwCpuName()).thenReturn(" CpuName ");
        Mockito.when(hardwareInfo.getHwCpuArchitecture()).thenReturn(" CpuArch ");
        Mockito.when(hardwareInfo.getHwCpuLogicalCoreCount()).thenReturn(" LogicalCores ");
        Mockito.when(hardwareInfo.getHwCpuPhysicalCoreCount()).thenReturn(" PhysicalCores ");
        Mockito.when(hardwareInfo.getHwCpuSocketCount()).thenReturn(" Sockets ");
        Mockito.when(hardwareInfo.getHwMemory()).thenReturn(" Memory ");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Assertions.assertEquals("OsFamily", detailedSystemInformation.toJsonObject().get("operatingSystemFamily"));
        Assertions.assertEquals("OsArch-Bit", detailedSystemInformation.toJsonObject().get("operatingSystemArchitecture"));
        Assertions.assertEquals("OsVersion", detailedSystemInformation.toJsonObject().get("operatingSystemVersion"));
        Assertions.assertEquals("OsCodeName", detailedSystemInformation.toJsonObject().get("operatingSystemCodeName"));
        Assertions.assertEquals("OsBuild", detailedSystemInformation.toJsonObject().get("operatingSystemBuildNumber"));
        Assertions.assertEquals("CpuName", detailedSystemInformation.toJsonObject().get("cpuName"));
        Assertions.assertEquals("CpuArch", detailedSystemInformation.toJsonObject().get("cpuArchitecture"));
        Assertions.assertEquals("LogicalCores", detailedSystemInformation.toJsonObject().get("cpuLogicalCores"));
        Assertions.assertEquals("PhysicalCores", detailedSystemInformation.toJsonObject().get("cpuPhysicalCores"));
        Assertions.assertEquals("Sockets", detailedSystemInformation.toJsonObject().get("cpuSockets"));
        Assertions.assertEquals("Memory", detailedSystemInformation.toJsonObject().get("memory"));


        // valid values
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("linux");
        Mockito.when(hardwareInfo.getOsFamily()).thenReturn(" OsFamily ");
        Mockito.when(hardwareInfo.getOsArchitecture()).thenReturn("OsArch");
        Mockito.when(hardwareInfo.getOsVersion()).thenReturn("OsVersion");
        Mockito.when(hardwareInfo.getOsCodeName()).thenReturn("OsCodeName");
        Mockito.when(hardwareInfo.getOsBuildNumber()).thenReturn("OsBuild");
        Mockito.when(hardwareInfo.getHwCpuName()).thenReturn("CpuName");
        Mockito.when(hardwareInfo.getHwCpuArchitecture()).thenReturn("CpuArch");
        Mockito.when(hardwareInfo.getHwCpuLogicalCoreCount()).thenReturn("LogicalCores");
        Mockito.when(hardwareInfo.getHwCpuPhysicalCoreCount()).thenReturn("PhysicalCores");
        Mockito.when(hardwareInfo.getHwCpuSocketCount()).thenReturn("Sockets");
        Mockito.when(hardwareInfo.getHwMemory()).thenReturn("Memory");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Assertions.assertEquals("OsFamily", detailedSystemInformation.toJsonObject().get("operatingSystemFamily"));
        Assertions.assertEquals("OsArch-Bit", detailedSystemInformation.toJsonObject().get("operatingSystemArchitecture"));
        Assertions.assertEquals("OsVersion", detailedSystemInformation.toJsonObject().get("operatingSystemVersion"));
        Assertions.assertEquals("OsCodeName", detailedSystemInformation.toJsonObject().get("operatingSystemCodeName"));
        Assertions.assertEquals("OsBuild", detailedSystemInformation.toJsonObject().get("operatingSystemBuildNumber"));
        Assertions.assertEquals("CpuName", detailedSystemInformation.toJsonObject().get("cpuName"));
        Assertions.assertEquals("CpuArch", detailedSystemInformation.toJsonObject().get("cpuArchitecture"));
        Assertions.assertEquals("LogicalCores", detailedSystemInformation.toJsonObject().get("cpuLogicalCores"));
        Assertions.assertEquals("PhysicalCores", detailedSystemInformation.toJsonObject().get("cpuPhysicalCores"));
        Assertions.assertEquals("Sockets", detailedSystemInformation.toJsonObject().get("cpuSockets"));
        Assertions.assertEquals("Memory", detailedSystemInformation.toJsonObject().get("memory"));

        // linux
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("linux");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("linux-system");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("system-linux");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("Linux");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("LINUX");
        detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        Assertions.assertEquals(OperatingSystem.LINUX, detailedSystemInformation.toJsonObject().get("operatingSystem"));


    }
}
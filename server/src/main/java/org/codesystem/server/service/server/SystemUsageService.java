package org.codesystem.server.service.server;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.ServerApplication;
import org.codesystem.server.entity.SystemUsageEntity;
import org.codesystem.server.repository.SystemUsageRepository;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.server.GetStorageInformationResponse;
import org.codesystem.server.response.server.GetSystemUsageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.File;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class SystemUsageService {
    private final SystemUsageRepository systemUsageRepository;
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();


    public double getCpuUsage() {
        return hardwareAbstractionLayer.getProcessor().getSystemCpuLoad(500) * 100;
    }

    public long getTotalMemory() {
        return hardwareAbstractionLayer.getMemory().getTotal();
    }

    public long getAvailableMemory() {
        return hardwareAbstractionLayer.getMemory().getAvailable();
    }

    public void addNewEntry(double cpuUsage, long memoryTotal, long memoryAvailable) {
        systemUsageRepository.save(new SystemUsageEntity(cpuUsage, memoryTotal, memoryAvailable));
    }

    public ResponseEntity<ApiResponse> getAllEntries() {
        return ResponseEntity.status(HttpStatus.OK).body(new GetSystemUsageResponse(systemUsageRepository.findAllSorted()));
    }

    public ResponseEntity<ApiResponse> getLatest30Entries() {
        return ResponseEntity.status(HttpStatus.OK).body(new GetSystemUsageResponse(systemUsageRepository.findAllSorted().stream().limit(30).toList()));
    }

    public ResponseEntity<ApiResponse> getStorageInformation() {
        File packageLocation = Paths.get(ServerApplication.PACKAGE_LOCATION).toFile();
        return ResponseEntity.status(HttpStatus.OK).body(new GetStorageInformationResponse(packageLocation.getTotalSpace(), packageLocation.getUsableSpace()));
    }
}

package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.service.server.SystemUsageService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SystemUsageCollector {
    private final SystemUsageService systemUsageService;

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 10)
    @Async("collectSystemUsageTask")
    public void collectSystemLog() {
        systemUsageService.addNewEntry(Math.round(systemUsageService.getCpuUsage() * 100.0) / 100.0,
                systemUsageService.getTotalMemory(),
                systemUsageService.getAvailableMemory());
    }
}

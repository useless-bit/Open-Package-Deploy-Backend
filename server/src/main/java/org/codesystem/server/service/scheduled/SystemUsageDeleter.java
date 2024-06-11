package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.SystemUsageRepository;
import org.codesystem.server.service.server.LogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SystemUsageDeleter {
    private final SystemUsageRepository systemUsageRepository;
    private final LogService logService;

    @Value("${opd.system-usage.delete-threshold}")
    private long systemUsageDeletionThreshold;

    @Scheduled(timeUnit = TimeUnit.DAYS, fixedDelay = 1)
    @Async("deleteSystemUsageTask")
    public void deleteOldSystemUsageEntries() {
        if (systemUsageDeletionThreshold > 0) {
            int deletedEntries = systemUsageRepository.deleteAllOlderThan(Instant.now().minus(systemUsageDeletionThreshold, ChronoUnit.SECONDS));
            if (deletedEntries > 0) {
                logService.addEntry(Severity.INFO, "Deleted " + deletedEntries + " old System-Usage Entries");
            }
        }
    }
}

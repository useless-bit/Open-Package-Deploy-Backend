package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.LogRepository;
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
public class LogDeleter {
    private final LogRepository logRepository;
    private final LogService logService;

    @Value("${opd.log.delete-threshold}")
    private long logDeletionThreshold;

    @Scheduled(timeUnit = TimeUnit.DAYS, fixedDelay = 1)
    @Async("deleteLogsTask")
    public void deleteOldLogs() {
        if (logDeletionThreshold > 0) {
            int deletedEntries = logRepository.deleteAllOlderThan(Instant.now().minus(logDeletionThreshold, ChronoUnit.SECONDS));
            if (deletedEntries > 0) {
                logService.addEntry(Severity.INFO, "Deleted " + deletedEntries + " old Logs");
            }
        }
    }
}

package org.codesystem.server.service.server;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.LogEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.LogRepository;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.server.ServerLogListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {
    private final LogRepository logRepository;
    private final Logger logger = LoggerFactory.getLogger(LogService.class);

    public void addEntry(Severity severity, String message) {
        LogEntity logEntity = logRepository.save(new LogEntity(severity, message));
        switch (logEntity.getSeverity()) {
            case ERROR -> logger.error(logEntity.getMessage());
            case WARNING -> logger.warn(logEntity.getMessage());
            case INFO -> logger.info(logEntity.getMessage());
        }
    }

    public ResponseEntity<ApiResponse> getAllEntries() {
        List<LogEntity> logEntities = logRepository.findAllSorted();
        return ResponseEntity.status(HttpStatus.OK).body(new ServerLogListResponse(logEntities));
    }
}

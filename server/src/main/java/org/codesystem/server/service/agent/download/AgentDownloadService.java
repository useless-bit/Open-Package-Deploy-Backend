package org.codesystem.server.service.agent.download;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.service.server.LogService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentDownloadService {
    private final ResourceLoader resourceLoader;
    private final LogService logService;

    public ResponseEntity<byte[]> download() {
        Resource resource = resourceLoader.getResource("classpath:agent/Agent.jar");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Agent.jar");
        try {
            return ResponseEntity.ok().headers(httpHeaders).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource.getContentAsByteArray());
        } catch (Exception e) {
            logService.addEntry(Severity.ERROR, "Failed to serve Agent-Download: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

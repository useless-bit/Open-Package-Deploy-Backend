package org.codesystem.server.service.agent.download;

import lombok.RequiredArgsConstructor;
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

    public ResponseEntity<byte[]> download() {
        Resource resource = resourceLoader.getResource("classpath:agent/Agent.jar");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Agent.jar");
        try {
            return ResponseEntity.ok().headers(httpHeaders).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource.getContentAsByteArray());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

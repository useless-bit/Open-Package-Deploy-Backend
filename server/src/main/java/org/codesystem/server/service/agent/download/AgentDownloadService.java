package org.codesystem.server.service.agent.download;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

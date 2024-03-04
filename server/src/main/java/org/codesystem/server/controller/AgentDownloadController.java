package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.service.agent.download.AgentDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent Download")
@RestController
@RequestMapping("/download/agent")
@RequiredArgsConstructor
public class AgentDownloadController {
    private final AgentDownloadService agentDownloadService;

    @GetMapping()
    public ResponseEntity<byte[]> downloadAgent() {
        return agentDownloadService.download();
    }
}

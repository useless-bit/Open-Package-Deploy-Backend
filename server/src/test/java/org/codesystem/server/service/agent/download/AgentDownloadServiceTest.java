package org.codesystem.server.service.agent.download;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentDownloadServiceTest {
    AgentDownloadService agentDownloadService;
    ResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        resourceLoader = Mockito.mock(ResourceLoader.class);
        agentDownloadService = new AgentDownloadService(resourceLoader);
    }

    @Test
    void download_noFile() {
        ResponseEntity responseEntity = agentDownloadService.download();
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    void download_valid() {
        Mockito.when(resourceLoader.getResource(Mockito.any())).thenReturn(new ClassPathResource("Test-File"));
        ResponseEntity responseEntity = agentDownloadService.download();
        byte[] response = (byte[]) responseEntity.getBody();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertArrayEquals("Test File Content".getBytes(StandardCharsets.UTF_8), response);
    }
}
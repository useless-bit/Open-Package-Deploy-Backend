package org.codesystem.server.service.server;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.LogRepository;
import org.codesystem.server.repository.SystemUsageRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemUsageServiceTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    SystemUsageRepository systemUsageRepository;
    SystemUsageService systemUsageService;

    @BeforeAll
    public static void init() {
        DB = new MariaDB4jSpringService();
        DB.setDefaultPort(3307);
        DB.setDefaultOsUser("root");
        DB.start();
    }

    @AfterAll
    public static void cleanupDB() {
        DB.stop();
    }

    @BeforeEach
    void setUp() {
        systemUsageService = new SystemUsageService(systemUsageRepository);
    }

    @AfterEach
    void tearDown() {
        systemUsageRepository.deleteAll();
    }

    @Test
    void getCpuUsage() {
        Assertions.assertDoesNotThrow(() -> systemUsageService.getCpuUsage());
    }
    @Test
    void getTotalMemory() {
        Assertions.assertDoesNotThrow(() -> systemUsageService.getTotalMemory());
    }
    @Test
    void getAvailableMemory() {
        Assertions.assertDoesNotThrow(() -> systemUsageService.getAvailableMemory());
    }

    void addEntry() {
        systemUsageService.addNewEntry(0.0, 100, 50);
        Assertions.assertEquals(1, systemUsageRepository.findAll().size());
    }

    @Test
    void getAllEntries() {
        ResponseEntity responseEntity = systemUsageService.getAllEntries();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
    @Test
    void getLatest30Entries() {
        ResponseEntity responseEntity = systemUsageService.getLatest30Entries();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
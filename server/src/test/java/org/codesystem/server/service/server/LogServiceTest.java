package org.codesystem.server.service.server;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.LogRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Objects;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LogServiceTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    LogRepository logRepository;
    LogService logService;

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
        logService = new LogService(logRepository);
    }

    @AfterEach
    void tearDown() {
        logRepository.deleteAll();
    }

    @Test
    void addEntry() {
        logService.addEntry(Severity.ERROR, "");
        Assertions.assertEquals(1, logRepository.findAll().size());
        logService.addEntry(Severity.WARNING, "");
        Assertions.assertEquals(2, logRepository.findAll().size());
        logService.addEntry(Severity.INFO, "");
        Assertions.assertEquals(3, logRepository.findAll().size());
    }

    @Test
    void getAllEntries() {
        ResponseEntity responseEntity = logService.getAllEntries();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
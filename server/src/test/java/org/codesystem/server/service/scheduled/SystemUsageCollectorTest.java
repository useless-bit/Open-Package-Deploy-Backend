package org.codesystem.server.service.scheduled;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.repository.SystemUsageRepository;
import org.codesystem.server.service.server.SystemUsageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemUsageCollectorTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    SystemUsageRepository systemUsageRepository;
    @Autowired
    SystemUsageService systemUsageService;
    SystemUsageCollector systemUsageCollector;

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
        systemUsageCollector = new SystemUsageCollector(systemUsageService);
    }

    @AfterEach
    void tearDown() {
        systemUsageRepository.deleteAll();
    }

    @Test
    void collectSystemLog() {
        Assertions.assertDoesNotThrow(() -> systemUsageCollector.collectSystemLog());
        Assertions.assertNotNull(systemUsageRepository.findAll());
    }
}
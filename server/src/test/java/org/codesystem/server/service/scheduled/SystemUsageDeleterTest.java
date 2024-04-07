package org.codesystem.server.service.scheduled;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.LogEntity;
import org.codesystem.server.entity.SystemUsageEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.SystemUsageRepository;
import org.codesystem.server.service.server.LogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemUsageDeleterTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    SystemUsageRepository systemUsageRepository;
    @Autowired
    LogService logService;
    SystemUsageDeleter systemUsageDeleter;

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
        systemUsageDeleter = new SystemUsageDeleter(systemUsageRepository, logService);
    }

    @AfterEach
    void tearDown() {
        systemUsageRepository.deleteAll();
    }

    @Test
    void deleteOldSystemUsageEntries() throws InterruptedException {
        // no entry
        Assertions.assertDoesNotThrow(() -> systemUsageDeleter.deleteOldSystemUsageEntries());
        Assertions.assertEquals(0, systemUsageRepository.findAll().size());

        // no to delete
        systemUsageRepository.save(new SystemUsageEntity(0.0, 100, 50));
        Assertions.assertDoesNotThrow(() -> systemUsageDeleter.deleteOldSystemUsageEntries());
        Assertions.assertEquals(1, systemUsageRepository.findAll().size());

        // deletion disabled
        ReflectionTestUtils.setField(systemUsageDeleter, "systemUsageDeletionThreshold", 0);
        Assertions.assertDoesNotThrow(() -> systemUsageDeleter.deleteOldSystemUsageEntries());
        Assertions.assertEquals(1, systemUsageRepository.findAll().size());

        // delete
        ReflectionTestUtils.setField(systemUsageDeleter, "systemUsageDeletionThreshold", 3);
        Thread.sleep(5000);
        Assertions.assertDoesNotThrow(() -> systemUsageDeleter.deleteOldSystemUsageEntries());
        Assertions.assertEquals(0, systemUsageRepository.findAll().size());
    }
}
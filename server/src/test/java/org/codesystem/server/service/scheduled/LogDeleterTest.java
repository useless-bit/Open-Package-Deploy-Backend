package org.codesystem.server.service.scheduled;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.LogEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.LogRepository;
import org.codesystem.server.service.server.LogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LogDeleterTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    LogRepository logRepository;
    @Autowired
    LogService logService;
    LogDeleter logDeleter;
    @Autowired
    private ConfigurableEnvironment environment;

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
        logDeleter = new LogDeleter(logRepository, logService);
    }

    @AfterEach
    void tearDown() {
        logRepository.deleteAll();
    }

    @Test
    void deletePackage() throws InterruptedException, NoSuchFieldException {
        // no log entry
        Assertions.assertDoesNotThrow(() -> logDeleter.deletePackage());
        Assertions.assertEquals(0, logRepository.findAll().size());

        // no log to delete
        logRepository.save(new LogEntity(Severity.INFO, "Test Entry"));
        Assertions.assertDoesNotThrow(() -> logDeleter.deletePackage());
        Assertions.assertEquals(1, logRepository.findAll().size());
        Assertions.assertEquals("Test Entry", logRepository.findAll().get(0).getMessage());

        // deletion disabled
        ReflectionTestUtils.setField(logDeleter, "logDeletionThreshold", 0);
        Assertions.assertDoesNotThrow(() -> logDeleter.deletePackage());
        Assertions.assertEquals(1, logRepository.findAll().size());
        Assertions.assertEquals("Test Entry", logRepository.findAll().get(0).getMessage());

        // delete log
        ReflectionTestUtils.setField(logDeleter, "logDeletionThreshold", 3);
        Thread.sleep(5000);
        Assertions.assertDoesNotThrow(() -> logDeleter.deletePackage());
        Assertions.assertEquals(1, logRepository.findAll().size());
        Assertions.assertEquals("Deleted 1 old Logs", logRepository.findAll().get(0).getMessage());
    }
}
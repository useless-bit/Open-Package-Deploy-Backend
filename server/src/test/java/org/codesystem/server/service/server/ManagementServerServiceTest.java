package org.codesystem.server.service.server;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.server.ServerDeploymentValidationRequest;
import org.codesystem.server.request.server.ServerAgentInstallRetryRequest;
import org.codesystem.server.request.server.ServerAgentUpdateIntervalRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Objects;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManagementServerServiceTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    ServerRepository serverRepository;
    LogService logService;
    ManagementServerService managementServerService;


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
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("Agent Checksum");
        serverEntity.setPrivateKeyBase64("Server Private Key");
        serverEntity.setPublicKeyBase64("Server Public Key");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverRepository.save(serverEntity);

        logService = Mockito.mock(LogService.class);

        managementServerService = new ManagementServerService(serverRepository, logService);
    }

    @AfterEach
    void tearDown() {
        serverRepository.deleteAll();
    }

    @Test
    void getRegistrationToken() {
        ResponseEntity responseEntity = managementServerService.getRegistrationToken();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Registration Token", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("registrationToken"));
    }

    @Test
    void updateRegistrationToken() {
        ResponseEntity responseEntity = managementServerService.updateRegistrationToken();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Assertions.assertNotEquals("Registration Token", serverEntity.getAgentRegistrationToken());
        Assertions.assertNotNull(serverEntity.getAgentRegistrationToken());
    }

    @Test
    void getUpdateInterval() {
        ResponseEntity responseEntity = managementServerService.getUpdateInterval();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(60, new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getInt("updateInterval"));
    }

    @Test
    void setUpdateInterval() {
        ResponseEntity responseEntity = managementServerService.setUpdateInterval(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementServerService.setUpdateInterval(new ServerAgentUpdateIntervalRequest(null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementServerService.setUpdateInterval(new ServerAgentUpdateIntervalRequest(0));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementServerService.setUpdateInterval(new ServerAgentUpdateIntervalRequest(500));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Assertions.assertEquals(500, serverEntity.getAgentUpdateInterval());
    }

    @Test
    void getInstallRetryInterval() {
        ResponseEntity responseEntity = managementServerService.getInstallRetryInterval();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(21600, new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getInt("installRetryInterval"));
    }

    @Test
    void setInstallRetryInterval() {
        ResponseEntity responseEntity = managementServerService.setInstallRetryInterval(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementServerService.setInstallRetryInterval(new ServerAgentInstallRetryRequest(null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementServerService.setInstallRetryInterval(new ServerAgentInstallRetryRequest(0));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementServerService.setInstallRetryInterval(new ServerAgentInstallRetryRequest(500));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Assertions.assertEquals(500, serverEntity.getAgentInstallRetryInterval());
    }

    @Test
    void getAgentChecksum() {
        ResponseEntity responseEntity = managementServerService.getAgentChecksum();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent Checksum", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("agentChecksum"));
    }

    @Test
    void getDeploymentValidationInterval() {
        ResponseEntity responseEntity = managementServerService.getDeploymentValidationInterval();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(43200, new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getInt("deploymentValidationInterval"));
    }

    @Test
    void setDeploymentValidationInterval() {
        ResponseEntity responseEntity = managementServerService.setDeploymentValidationInterval(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementServerService.setDeploymentValidationInterval(new ServerDeploymentValidationRequest(null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementServerService.setDeploymentValidationInterval(new ServerDeploymentValidationRequest(0));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid interval", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementServerService.setDeploymentValidationInterval(new ServerDeploymentValidationRequest(500));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Assertions.assertEquals(500, serverEntity.getDeploymentValidationInterval());
    }

    @Test
    void resetDeploymentValidationInterval() {
        ResponseEntity responseEntity = managementServerService.resetDeploymentValidationInterval();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Assertions.assertNull(serverEntity.getLastDeploymentValidation());
    }

    @Test
    void getLastDeploymentValidationTimestamp() {
        ResponseEntity responseEntity = managementServerService.getLastDeploymentValidationTimestamp();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
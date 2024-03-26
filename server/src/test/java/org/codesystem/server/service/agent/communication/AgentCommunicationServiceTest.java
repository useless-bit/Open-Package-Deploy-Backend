package org.codesystem.server.service.agent.communication;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.agent.AgentEncryptedRequest;
import org.codesystem.server.response.agent.AgentEncryptedResponse;
import org.codesystem.server.response.agent.communication.AgentCheckForUpdateResponse;
import org.codesystem.server.utility.RequestUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentCommunicationServiceTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    AgentRepository agentRepository;
    @Autowired
    DeploymentRepository deploymentRepository;
    @Autowired
    ServerRepository serverRepository;
    RequestUtility requestUtility;
    ResourceLoader resourceLoader;
    AgentCommunicationService agentCommunicationService;

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
        requestUtility = Mockito.mock(RequestUtility.class);
        resourceLoader = Mockito.mock(ResourceLoader.class);
        agentCommunicationService = new AgentCommunicationService(agentRepository, deploymentRepository, requestUtility, serverRepository, resourceLoader);
    }

    @AfterEach
    void tearDown() {
        agentRepository.deleteAll();
        deploymentRepository.deleteAll();
        serverRepository.deleteAll();
    }

    @Test
    void checkForUpdates_invalidRequest() {
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(null);
        responseEntity = agentCommunicationService.checkForUpdates(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        JSONObject jsonObject = new JSONObject();
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        responseEntity = agentCommunicationService.checkForUpdates(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void checkForUpdates_invalidAgent() {
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Key", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void checkForUpdates_emptyAgentCheckForUpdateRequest() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            System.out.println(invocationOnMock.getArgument(0).toString());
            return new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString());
        });
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        System.out.println(jsonResponse);
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertEquals(false, jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
    }
}
package org.codesystem.server.service.agent.registration;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.agent.registration.AgentRegistrationRequest;
import org.codesystem.server.request.agent.registration.AgentVerificationRequest;
import org.codesystem.server.utility.CryptoUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentRegistrationServiceTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    ServerRepository serverRepository;
    @Autowired
    AgentRepository agentRepository;
    AgentRegistrationService agentRegistrationService;
    CryptoUtility cryptoUtility;

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
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverRepository.save(serverEntity);
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        agentRegistrationService = new AgentRegistrationService(agentRepository, serverRepository, cryptoUtility);
    }

    @AfterEach
    void tearDown() {
        serverRepository.deleteAll();
        agentRepository.deleteAll();
    }


    @Test
    void addNewAgent_invalidRequest() {
        ResponseEntity responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest(null, null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("", null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("   ", null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", "", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", "   ", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", "Agent Name", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", "Agent Name", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", "Agent Name", "   "));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void addNewAgent_invalidRegistrationToken() {
        ResponseEntity responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("PublicKey", "Agent Name", "Invalid Token"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void addNewAgent_agentPublicKeyAlreadyRegistered() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setRegistrationCompleted(true);
        agentEntity = agentRepository.save(agentEntity);
        ResponseEntity responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest(agentEntity.getPublicKeyBase64(), "Agent Name", "Registration Token"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("A Agent with this public key is already registered", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void addNewAgent_updateUnregisteredAgent() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        Mockito.when(cryptoUtility.encryptECC(Mockito.any(), Mockito.any())).thenReturn("Encrypted Message".getBytes(StandardCharsets.UTF_8));
        ResponseEntity responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest(agentEntity.getPublicKeyBase64(), "Agent Name", "Registration Token"));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonObjectResponse = new JSONObject(responseEntity.getBody());
        Assertions.assertEquals("Public Key", jsonObjectResponse.getString("publicKeyBase64"));
        Assertions.assertArrayEquals("Encrypted Message".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObjectResponse.getString("encryptedValidationToken")));
        agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEntity.getPublicKeyBase64());
        Assertions.assertEquals("Agent Name", agentEntity.getName());
        Assertions.assertNotNull(agentEntity.getValidationToken());
    }

    @Test
    void addNewAgent_newAgent() {
        Mockito.when(cryptoUtility.encryptECC(Mockito.any(), Mockito.any())).thenReturn("Encrypted Message".getBytes(StandardCharsets.UTF_8));
        ResponseEntity responseEntity = agentRegistrationService.addNewAgent(new AgentRegistrationRequest("publicKeyForAgent", " Agent Name ", "Registration Token"));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonObjectResponse = new JSONObject(responseEntity.getBody());
        Assertions.assertEquals("Public Key", jsonObjectResponse.getString("publicKeyBase64"));
        Assertions.assertArrayEquals("Encrypted Message".getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(jsonObjectResponse.getString("encryptedValidationToken")));
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64("publicKeyForAgent");
        Assertions.assertEquals("Agent Name", agentEntity.getName());
        Assertions.assertNotNull(agentEntity.getValidationToken());
    }

    @Test
    void verifyNewAgent_invalidRequest() {
        ResponseEntity responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest(null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest("", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest("   ", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest("PublicKey", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest("PublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest("PublicKey", "   "));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void verifyNewAgent_invalidAgent() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setRegistrationCompleted(true);
        agentEntity = agentRepository.save(agentEntity);
        ResponseEntity responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest("invalidPublicKey", Base64.getEncoder().encodeToString("VerificationToken".getBytes(StandardCharsets.UTF_8))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Cannot verify Agent", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest(agentEntity.getPublicKeyBase64(), Base64.getEncoder().encodeToString("VerificationToken".getBytes(StandardCharsets.UTF_8))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Cannot verify Agent", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void verifyNewAgent_invalidVerificationToken() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn("Validation Token");
        ResponseEntity responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest(agentEntity.getPublicKeyBase64(), Base64.getEncoder().encodeToString("VerificationToken".getBytes(StandardCharsets.UTF_8))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Cannot verify Agent", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void verifyNewAgent_validVerificationToken() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setValidationToken("Validation Token");
        agentEntity = agentRepository.save(agentEntity);
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn("Validation Token");
        ResponseEntity responseEntity = agentRegistrationService.verifyNewAgent(new AgentVerificationRequest(agentEntity.getPublicKeyBase64(), Base64.getEncoder().encodeToString("VerificationToken".getBytes(StandardCharsets.UTF_8))));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEntity.getPublicKeyBase64());
        Assertions.assertTrue(agentEntity.isRegistrationCompleted());
    }


}
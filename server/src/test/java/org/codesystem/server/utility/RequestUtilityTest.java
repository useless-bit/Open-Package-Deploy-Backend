package org.codesystem.server.utility;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.request.agent.AgentEncryptedRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RequestUtilityTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    AgentRepository agentRepository;
    CryptoUtility cryptoUtility;
    private RequestUtility requestUtility;

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
        MockitoAnnotations.openMocks(this);
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        requestUtility = new RequestUtility(agentRepository, cryptoUtility);
    }

    @AfterEach
    void tearDown() {
        agentRepository.deleteAll();
    }

    @Test
    void validateRequest_invalidAgentEncryptedRequest() {
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest()));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(null, null)));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("", null)));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(null, "")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("   ", null)));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(null, "   ")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("invalid", null)));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("invalid", "")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("invalid", "   ")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(null, "invalid")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("", "invalid")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest("   ", "invalid")));
    }

    @Test
    void validateRequest_invalidAgent() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test Agent");
        agentEntity.setRegistrationCompleted(false);
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)));
        agentRepository.save(agentEntity);
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("WrongAgentPublicKey".getBytes(StandardCharsets.UTF_8)), "Test Message")));
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), "Test Message")));
    }

    @Test
    void validateRequest_invalidDecryptedMessage() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test Agent");
        agentEntity.setRegistrationCompleted(true);
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)));
        agentRepository.save(agentEntity);
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), "Test Message")));
        JSONObject jsonObject = new JSONObject();
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
        jsonObject.put("key", "value");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
        jsonObject.put("signature", "signature");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void validateRequest_invalidTimestamp() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test Agent");
        agentEntity.setRegistrationCompleted(true);
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)));
        agentRepository.save(agentEntity);
        Instant instant = Instant.now();
        JSONObject jsonObject = new JSONObject().put("key", "value").put("signature", "").put("timestamp", "timestamp");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
        jsonObject = new JSONObject().put("key", "value").put("signature", "").put("timestamp", instant.minus(310, ChronoUnit.SECONDS));
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
        jsonObject = new JSONObject().put("key", "value").put("signature", "").put("timestamp", instant.plus(310, ChronoUnit.SECONDS));
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void validateRequest_invalidSignature() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test Agent");
        agentEntity.setRegistrationCompleted(true);
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)));
        agentRepository.save(agentEntity);
        Instant instant = Instant.now();
        JSONObject jsonObject = new JSONObject().put("key", "value").put("signature", "").put("timestamp", instant);
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
        jsonObject = new JSONObject().put("key", "value").put("signature", "   ").put("timestamp", instant);
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
        jsonObject = new JSONObject().put("key", "value").put("signature", "signature").put("timestamp", instant);
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(cryptoUtility.verifySignatureECC(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);
        Assertions.assertNull(requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void validateRequest_valid() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test Agent");
        agentEntity.setRegistrationCompleted(true);
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)));
        agentRepository.save(agentEntity);
        Instant instant = Instant.now();
        JSONObject jsonObject = new JSONObject().put("key", "value").put("signature", "signature").put("timestamp", instant);
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(cryptoUtility.verifySignatureECC(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Assertions.assertEquals(new JSONObject().put("key", "value").put("timestamp", instant).toString(), requestUtility.validateRequest(new AgentEncryptedRequest(Base64.getEncoder().encodeToString("AgentPublicKey".getBytes(StandardCharsets.UTF_8)), Base64.getEncoder().encodeToString("Test Message".getBytes(StandardCharsets.UTF_8)))).toString());

    }

    @Test
    void generateAgentEncryptedResponse_invalid() {
        JSONObject jsonObject = new JSONObject();
        Assertions.assertNull(requestUtility.generateAgentEncryptedResponse(null, null));
        Assertions.assertNull(requestUtility.generateAgentEncryptedResponse(jsonObject, null));
        jsonObject.put("key", "value");
        Assertions.assertNull(requestUtility.generateAgentEncryptedResponse(jsonObject, null));
    }

    @Test
    void generateAgentEncryptedResponse_valid() {
        JSONObject jsonObject = new JSONObject().put("key", "value");
        AgentEntity agentEntity = new AgentEntity();
        // checks if timestamp is present
        Mockito.when(cryptoUtility.createSignatureECC(Mockito.any())).then(invocationOnMock -> {
            JSONObject jsonObjectMock = new JSONObject((String) invocationOnMock.getArgument(0));
            if (jsonObjectMock.isEmpty() || jsonObjectMock.isNull("timestamp")) {
                return null;
            } else {
                return "signature".getBytes(StandardCharsets.UTF_8);
            }
        });
        // checks if timestamp and signature are present
        Mockito.when(cryptoUtility.encryptECC(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            System.out.println(Arrays.toString(invocationOnMock.getArguments()));
            byte[] argument = invocationOnMock.getArgument(0);
            JSONObject jsonObjectMock = new JSONObject(new String(argument));
            if (jsonObjectMock.isEmpty() || jsonObjectMock.isNull("timestamp") || jsonObjectMock.isNull("signature")) {
                return null;
            } else {
                return "Decrypted Message".getBytes(StandardCharsets.UTF_8);
            }
        });
        Assertions.assertEquals(Base64.getEncoder().encodeToString("Decrypted Message".getBytes(StandardCharsets.UTF_8)), requestUtility.generateAgentEncryptedResponse(jsonObject, agentEntity).getMessage());
    }

}
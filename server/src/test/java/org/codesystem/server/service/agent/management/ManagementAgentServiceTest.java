package org.codesystem.server.service.agent.management;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.agent.management.AgentUpdateRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManagementAgentServiceTest {
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
    PackageRepository packageRepository;
    ManagementAgentService managementAgentService;
    AgentEntity agentEntityOne;
    AgentEntity agentEntityTwo;


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
        agentEntityOne = new AgentEntity();
        agentEntityOne.setPublicKeyBase64("Agent One Public Key");
        agentEntityOne.setName("Agent One");
        agentEntityOne = agentRepository.save(agentEntityOne);
        agentEntityTwo = new AgentEntity();
        agentEntityTwo.setPublicKeyBase64("Agent Two Public Key");
        agentEntityTwo.setName("Agent Two");
        agentEntityTwo = agentRepository.save(agentEntityTwo);
        managementAgentService = new ManagementAgentService(agentRepository, deploymentRepository);
    }

    @AfterEach
    void tearDown() {
        deploymentRepository.deleteAll();
        agentRepository.deleteAll();
    }

    @Test
    void getAllAgents() {
        ResponseEntity responseEntity = managementAgentService.getAllAgents();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(agentRepository.findAll().get(0).getUuid(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("agents").get(0).toString()).getString("uuid"));
        Assertions.assertEquals(agentRepository.findAll().get(1).getUuid(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("agents").get(1).toString()).getString("uuid"));
    }

    @Test
    void getAgent() {
        ResponseEntity responseEntity = managementAgentService.getAgent("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = managementAgentService.getAgent(agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent One", new JSONObject(responseEntity.getBody()).getString("name"));
        responseEntity = managementAgentService.getAgent(agentEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent Two", new JSONObject(responseEntity.getBody()).getString("name"));
    }

    @Test
    void updateAgent_invalidRequest() {
        ResponseEntity responseEntity = managementAgentService.updateAgent(null, new AgentUpdateRequest());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = managementAgentService.updateAgent(null, new AgentUpdateRequest(null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = managementAgentService.updateAgent(null, new AgentUpdateRequest(""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = managementAgentService.updateAgent(null, new AgentUpdateRequest("   "));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = managementAgentService.updateAgent(null, new AgentUpdateRequest("New Name"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(responseEntity.getBody()).getString("message"));
        responseEntity = managementAgentService.updateAgent("invalidAgent", new AgentUpdateRequest("New Name"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void updateAgent_validRequest() {
        ResponseEntity responseEntity = managementAgentService.updateAgent(agentEntityOne.getUuid(), new AgentUpdateRequest(" New Agent Name "));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("New Agent Name", agentRepository.findFirstByPublicKeyBase64(agentEntityOne.getPublicKeyBase64()).getName());
    }

    @Test
    void deleteAgent() {
        ResponseEntity responseEntity = managementAgentService.deleteAgent("invalidAgent");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(responseEntity.getBody()).getString("message"));

        responseEntity = managementAgentService.deleteAgent(agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64(agentEntityOne.getPublicKeyBase64()));

        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setName("Package");
        packageEntity.setChecksumEncrypted("CheckSum");
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.UNKNOWN);
        packageEntity = packageRepository.save(packageEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntityTwo);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        responseEntity = managementAgentService.deleteAgent(agentEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64(agentEntityTwo.getPublicKeyBase64()));
        Assertions.assertNull(deploymentRepository.findFirstByUuid(deploymentEntity.getUuid()));
    }
}
package org.codesystem.server.service.deployment;

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
import org.codesystem.server.service.agent.management.ManagementAgentService;
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

class ManagementDeploymentServiceTest {
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
    ManagementDeploymentService managementDeploymentService;
    PackageEntity packageEntityOne;
    PackageEntity packageEntityTwo;
    AgentEntity agentEntityOne;
    AgentEntity agentEntityTwo;
    DeploymentEntity deploymentEntityOne;
    DeploymentEntity deploymentEntityTwo;

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
        packageEntityOne = new PackageEntity();
        packageEntityOne.setChecksumPlaintext("Plaintext Checksum");
        packageEntityOne.setChecksumEncrypted("Encrypted Checksum");
        packageEntityOne.setTargetOperatingSystem(OperatingSystem.UNKNOWN);
        packageEntityOne.setName("Package One");
        packageEntityOne = packageRepository.save(packageEntityOne);
        packageEntityTwo = new PackageEntity();
        packageEntityTwo.setChecksumPlaintext("Plaintext Checksum");
        packageEntityTwo.setChecksumEncrypted("Encrypted Checksum");
        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.UNKNOWN);
        packageEntityTwo.setName("Package Two");
        packageEntityTwo = packageRepository.save(packageEntityTwo);

        agentEntityOne = new AgentEntity();
        agentEntityOne.setPublicKeyBase64("Agent One Public Key");
        agentEntityOne.setName("Agent One");
        agentEntityOne = agentRepository.save(agentEntityOne);
        agentEntityTwo = new AgentEntity();
        agentEntityTwo.setPublicKeyBase64("Agent Two Public Key");
        agentEntityTwo.setName("Agent Two");
        agentEntityTwo = agentRepository.save(agentEntityTwo);

        deploymentEntityOne = new DeploymentEntity();
        deploymentEntityOne.setPackageEntity(packageEntityOne);
        deploymentEntityOne.setAgentEntity(agentEntityOne);
        deploymentEntityOne = deploymentRepository.save(deploymentEntityOne);
        deploymentEntityTwo = new DeploymentEntity();
        deploymentEntityTwo.setPackageEntity(packageEntityTwo);
        deploymentEntityTwo.setAgentEntity(agentEntityTwo);
        deploymentEntityTwo = deploymentRepository.save(deploymentEntityTwo);

        managementDeploymentService = new ManagementDeploymentService(deploymentRepository, agentRepository, packageRepository);
    }

    @AfterEach
    void tearDown() {
        deploymentRepository.deleteAll();
        packageRepository.deleteAll();
        agentRepository.deleteAll();
    }

    @Test
    void getAllDeployments() {
        ResponseEntity responseEntity = managementDeploymentService.getAllDeployments();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(deploymentRepository.findAll().get(0).getAgentEntity().getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("deployments").get(0).toString()).getString("agentName"));
        Assertions.assertEquals(deploymentRepository.findAll().get(1).getAgentEntity().getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("deployments").get(1).toString()).getString("agentName"));
    }

    @Test
    void getDeployment() {
        ResponseEntity responseEntity = managementDeploymentService.getDeployment("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.getDeployment(deploymentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent One", new JSONObject(responseEntity.getBody()).getString("agentName"));
        responseEntity = managementDeploymentService.getDeployment(deploymentEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent Two", new JSONObject(responseEntity.getBody()).getString("agentName"));
    }
}
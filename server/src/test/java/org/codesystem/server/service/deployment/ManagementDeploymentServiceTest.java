package org.codesystem.server.service.deployment;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.deployment.CreateNewDeploymentRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
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

    @Test
    void createNewDeployment_invalidRequest() {
        ResponseEntity responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest("", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest("invalid UUID", null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), "invalid UUID"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void createNewDeployment_invalid() {
        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageEntityOne = packageRepository.save(packageEntityOne);
        ResponseEntity responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityOne.getUuid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not available for deployment", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityOne.getUuid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment already present", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityOne.getUuid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment already present", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.UNKNOWN);
        packageEntityTwo = packageRepository.save(packageEntityTwo);
        agentEntityOne.setOperatingSystem(OperatingSystem.UNKNOWN);
        agentEntityOne = agentRepository.save(agentEntityOne);
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityTwo.getUuid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid OS", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntityTwo = packageRepository.save(packageEntityTwo);
        agentEntityOne.setOperatingSystem(OperatingSystem.UNKNOWN);
        agentEntityOne = agentRepository.save(agentEntityOne);
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityTwo.getUuid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid OS", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.UNKNOWN);
        packageEntityTwo = packageRepository.save(packageEntityTwo);
        agentEntityOne.setOperatingSystem(OperatingSystem.LINUX);
        agentEntityOne = agentRepository.save(agentEntityOne);
        responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityTwo.getUuid()));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid OS", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void createNewDeployment_valid() {
        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntityTwo = packageRepository.save(packageEntityTwo);
        agentEntityOne.setOperatingSystem(OperatingSystem.LINUX);
        agentEntityOne = agentRepository.save(agentEntityOne);
        ResponseEntity responseEntity = managementDeploymentService.createNewDeployment(new CreateNewDeploymentRequest(agentEntityOne.getUuid(), packageEntityTwo.getUuid()));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        String deploymentUUID = new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("deploymentUUID");
        Assertions.assertNotNull(deploymentUUID);
        DeploymentEntity newDeploymentEntity = deploymentRepository.findFirstByUuid(deploymentUUID);
        Assertions.assertEquals("Agent One", newDeploymentEntity.getAgentEntity().getName());
        Assertions.assertEquals("Package Two", newDeploymentEntity.getPackageEntity().getName());
    }

    @Test
    void deleteDeployment() {
        ResponseEntity responseEntity = managementDeploymentService.deleteDeployment(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.deleteDeployment("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementDeploymentService.deleteDeployment(deploymentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNull(deploymentRepository.findFirstByUuid(deploymentEntityOne.getUuid()));
    }

    @Test
    void getAllDeploymentsForAgent() {
        ResponseEntity responseEntity = managementDeploymentService.getAllDeploymentsForAgent(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.getAllDeploymentsForAgent("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementDeploymentService.getAllDeploymentsForAgent(agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Package One", new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("deployments").get(0).toString()).getString("packageName"));
    }

    @Test
    void resetDeployment() {
        deploymentEntityOne.setDeployed(true);
        deploymentEntityOne.setLastDeploymentTimestamp(Instant.now());
        deploymentEntityOne = deploymentRepository.save(deploymentEntityOne);

        ResponseEntity responseEntity = managementDeploymentService.resetDeployment(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.resetDeployment("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Deployment not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementDeploymentService.resetDeployment(deploymentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntityOne = deploymentRepository.findFirstByUuid(deploymentEntityOne.getUuid());
        Assertions.assertFalse(deploymentEntityOne.isDeployed());
        Assertions.assertNull(deploymentEntityOne.getLastDeploymentTimestamp());
    }

    @Test
    void resetDeploymentForAgent() {
        deploymentEntityOne.setDeployed(true);
        deploymentEntityOne.setLastDeploymentTimestamp(Instant.now());
        deploymentEntityOne = deploymentRepository.save(deploymentEntityOne);

        ResponseEntity responseEntity = managementDeploymentService.resetDeploymentForAgent(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.resetDeploymentForAgent("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementDeploymentService.resetDeploymentForAgent(agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntityOne = deploymentRepository.findFirstByUuid(deploymentEntityOne.getUuid());
        Assertions.assertFalse(deploymentEntityOne.isDeployed());
        Assertions.assertNull(deploymentEntityOne.getLastDeploymentTimestamp());
    }

    @Test
    void resetDeploymentForPackage() {
        deploymentEntityOne.setDeployed(true);
        deploymentEntityOne.setLastDeploymentTimestamp(Instant.now());
        deploymentEntityOne = deploymentRepository.save(deploymentEntityOne);

        ResponseEntity responseEntity = managementDeploymentService.resetDeploymentForPackage(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementDeploymentService.resetDeploymentForPackage("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementDeploymentService.resetDeploymentForPackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntityOne = deploymentRepository.findFirstByUuid(deploymentEntityOne.getUuid());
        Assertions.assertFalse(deploymentEntityOne.isDeployed());
        Assertions.assertNull(deploymentEntityOne.getLastDeploymentTimestamp());
    }


}
package org.codesystem.server.service.scheduled;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.*;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.repository.*;
import org.codesystem.server.service.server.LogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeploymentValidatorTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    ServerRepository serverRepository;
    @Autowired
    DeploymentRepository deploymentRepository;
    @Autowired
    AgentRepository agentRepository;
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    LogService logService;
    DeploymentValidator deploymentValidator;
    PackageEntity packageEntityOne;
    PackageEntity packageEntityTwo;
    AgentEntity agentEntityOne;
    AgentEntity agentEntityTwo;
    GroupEntity groupEntityOne;
    GroupEntity groupEntityTwo;

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
        serverEntity.setDeploymentValidationInterval(100);
        serverEntity.setLastDeploymentValidation(null);
        serverRepository.save(serverEntity);

        packageEntityOne = new PackageEntity();
        packageEntityOne.setChecksumPlaintext("Plaintext Checksum");
        packageEntityOne.setChecksumEncrypted("Encrypted Checksum");
        packageEntityOne.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntityOne.setName("Package One");
        packageEntityOne = packageRepository.save(packageEntityOne);
        packageEntityTwo = new PackageEntity();
        packageEntityTwo.setChecksumPlaintext("Plaintext Checksum");
        packageEntityTwo.setChecksumEncrypted("Encrypted Checksum");
        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.WINDOWS);
        packageEntityTwo.setName("Package Two");
        packageEntityTwo = packageRepository.save(packageEntityTwo);

        agentEntityOne = new AgentEntity();
        agentEntityOne.setPublicKeyBase64("Agent One Public Key");
        agentEntityOne.setName("Agent One");
        agentEntityOne.setRegistrationCompleted(true);
        agentEntityOne.setOperatingSystem(OperatingSystem.LINUX);
        agentEntityOne = agentRepository.save(agentEntityOne);
        agentEntityTwo = new AgentEntity();
        agentEntityTwo.setPublicKeyBase64("Agent Two Public Key");
        agentEntityTwo.setName("Agent Two");
        agentEntityTwo.setRegistrationCompleted(true);
        agentEntityTwo.setOperatingSystem(OperatingSystem.WINDOWS);
        agentEntityTwo = agentRepository.save(agentEntityTwo);

        groupEntityOne = new GroupEntity("Group 1", "Desc for Group 1");
        groupEntityOne.addMember(agentEntityOne);
        groupEntityOne.addPackage(packageEntityOne);
        groupEntityOne.setOperatingSystem(OperatingSystem.LINUX);
        groupEntityOne = groupRepository.save(groupEntityOne);
        groupEntityTwo = new GroupEntity("Group 2", "Desc for Group 2");
        groupEntityTwo.addMember(agentEntityTwo);
        groupEntityTwo.addPackage(packageEntityTwo);
        groupEntityTwo.setOperatingSystem(OperatingSystem.WINDOWS);
        groupEntityTwo = groupRepository.save(groupEntityTwo);

        deploymentValidator = new DeploymentValidator(serverRepository, deploymentRepository, agentRepository, packageRepository, groupRepository, logService);
    }

    @AfterEach
    void tearDown() {
        groupRepository.deleteAll();
        deploymentRepository.deleteAll();
        agentRepository.deleteAll();
        packageRepository.deleteAll();
        serverRepository.deleteAll();
    }

    @Test
    void validateDeployments_updateInterval() {
        groupRepository.deleteAll();
        // null
        deploymentValidator.validateDeployments();
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Instant timestamp = serverEntity.getLastDeploymentValidation();
        Assertions.assertNotNull(timestamp);

        // no update
        deploymentValidator.validateDeployments();
        serverEntity = serverRepository.findAll().get(0);
        Assertions.assertEquals(timestamp, serverEntity.getLastDeploymentValidation());

        // update
        serverEntity = serverRepository.findAll().get(0);
        serverEntity.setDeploymentValidationInterval(0);
        serverRepository.save(serverEntity);
        deploymentValidator.validateDeployments();
        serverEntity = serverRepository.findAll().get(0);
        Assertions.assertNotEquals(timestamp, serverEntity.getLastDeploymentValidation());
        Assertions.assertNotNull(serverEntity.getLastDeploymentValidation());
    }

    @Test
    void validateDeployments_noAgentsNoPackages() {
        groupRepository.deleteAll();
        agentRepository.deleteAll();
        packageRepository.deleteAll();
        deploymentValidator.validateDeployments();
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Instant timestamp = serverEntity.getLastDeploymentValidation();
        Assertions.assertNotNull(timestamp);
    }

    @Test
    void validateDeployments_noPackages() {
        groupRepository.deleteAll();
        packageRepository.deleteAll();
        deploymentValidator.validateDeployments();
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Instant timestamp = serverEntity.getLastDeploymentValidation();
        Assertions.assertNotNull(timestamp);
    }

    @Test
    void validateDeployments_noAgents() {
        groupRepository.deleteAll();
        agentRepository.deleteAll();
        deploymentValidator.validateDeployments();
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        Instant timestamp = serverEntity.getLastDeploymentValidation();
        Assertions.assertNotNull(timestamp);
    }

    @Test
    void deleteDuplicateDeployments_noDirectDeployment() {
        groupEntityTwo.removePackage(packageEntityTwo);
        groupEntityTwo = groupRepository.save(groupEntityTwo);
        deploymentRepository.deleteAll();

        DeploymentEntity deploymentEntityDuplicateOne = new DeploymentEntity();
        deploymentEntityDuplicateOne.setPackageEntity(packageEntityOne);
        deploymentEntityDuplicateOne.setAgentEntity(agentEntityOne);
        deploymentEntityDuplicateOne.setDirectDeployment(false);
        deploymentRepository.save(deploymentEntityDuplicateOne);

        DeploymentEntity deploymentEntityDuplicateTwo = new DeploymentEntity();
        deploymentEntityDuplicateTwo.setPackageEntity(packageEntityOne);
        deploymentEntityDuplicateTwo.setAgentEntity(agentEntityOne);
        deploymentEntityDuplicateTwo.setDirectDeployment(false);
        deploymentRepository.save(deploymentEntityDuplicateTwo);

        Assertions.assertEquals(2, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(1, deploymentRepository.findAll().size());
        DeploymentEntity deploymentEntity = deploymentRepository.findAll().get(0);
        Assertions.assertFalse(deploymentEntity.isDirectDeployment());
        Assertions.assertEquals(agentEntityOne.getUuid(), deploymentEntity.getAgentEntity().getUuid());
        Assertions.assertEquals(packageEntityOne.getUuid(), deploymentEntity.getPackageEntity().getUuid());
    }

    @Test
    void deleteDuplicateDeployments_oneDirectDeployment() {
        groupEntityTwo.removePackage(packageEntityTwo);
        groupEntityTwo = groupRepository.save(groupEntityTwo);
        deploymentRepository.deleteAll();

        DeploymentEntity deploymentEntityDuplicateOne = new DeploymentEntity();
        deploymentEntityDuplicateOne.setPackageEntity(packageEntityOne);
        deploymentEntityDuplicateOne.setAgentEntity(agentEntityOne);
        deploymentEntityDuplicateOne.setDirectDeployment(false);
        deploymentRepository.save(deploymentEntityDuplicateOne);

        DeploymentEntity deploymentEntityDuplicateTwo = new DeploymentEntity();
        deploymentEntityDuplicateTwo.setPackageEntity(packageEntityOne);
        deploymentEntityDuplicateTwo.setAgentEntity(agentEntityOne);
        deploymentEntityDuplicateTwo.setDirectDeployment(true);
        deploymentRepository.save(deploymentEntityDuplicateTwo);

        Assertions.assertEquals(2, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(1, deploymentRepository.findAll().size());
        DeploymentEntity deploymentEntity = deploymentRepository.findAll().get(0);
        Assertions.assertTrue(deploymentEntity.isDirectDeployment());
        Assertions.assertEquals(agentEntityOne.getUuid(), deploymentEntity.getAgentEntity().getUuid());
        Assertions.assertEquals(packageEntityOne.getUuid(), deploymentEntity.getPackageEntity().getUuid());
    }

    @Test
    void deleteDuplicateDeployments_allDirectDeployment() {
        groupEntityTwo.removePackage(packageEntityTwo);
        groupEntityTwo = groupRepository.save(groupEntityTwo);
        deploymentRepository.deleteAll();

        DeploymentEntity deploymentEntityDuplicateOne = new DeploymentEntity();
        deploymentEntityDuplicateOne.setPackageEntity(packageEntityOne);
        deploymentEntityDuplicateOne.setAgentEntity(agentEntityOne);
        deploymentEntityDuplicateOne.setDirectDeployment(true);
        deploymentRepository.save(deploymentEntityDuplicateOne);

        DeploymentEntity deploymentEntityDuplicateTwo = new DeploymentEntity();
        deploymentEntityDuplicateTwo.setPackageEntity(packageEntityOne);
        deploymentEntityDuplicateTwo.setAgentEntity(agentEntityOne);
        deploymentEntityDuplicateTwo.setDirectDeployment(true);
        deploymentRepository.save(deploymentEntityDuplicateTwo);

        Assertions.assertEquals(2, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(1, deploymentRepository.findAll().size());
        DeploymentEntity deploymentEntity = deploymentRepository.findAll().get(0);
        Assertions.assertTrue(deploymentEntity.isDirectDeployment());
        Assertions.assertEquals(agentEntityOne.getUuid(), deploymentEntity.getAgentEntity().getUuid());
        Assertions.assertEquals(packageEntityOne.getUuid(), deploymentEntity.getPackageEntity().getUuid());
    }

    @Test
    void addMissingDeployment_noDeployment() {
        groupEntityOne.removePackage(packageEntityOne);
        groupEntityOne = groupRepository.save(groupEntityOne);
        groupEntityTwo.removePackage(packageEntityTwo);
        groupEntityTwo = groupRepository.save(groupEntityTwo);
        Assertions.assertEquals(0, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(0, deploymentRepository.findAll().size());
    }

    @Test
    void addMissingDeployment_deploymentAlreadyPresent() {
        DeploymentEntity deploymentEntityOne = new DeploymentEntity();
        deploymentEntityOne.setAgentEntity(agentEntityOne);
        deploymentEntityOne.setPackageEntity(packageEntityOne);
        deploymentEntityOne.setDirectDeployment(true);
        deploymentRepository.save(deploymentEntityOne);
        DeploymentEntity deploymentEntityTwo = new DeploymentEntity();
        deploymentEntityTwo.setAgentEntity(agentEntityTwo);
        deploymentEntityTwo.setPackageEntity(packageEntityTwo);
        deploymentEntityTwo.setDirectDeployment(false);
        deploymentRepository.save(deploymentEntityTwo);

        Assertions.assertEquals(2, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(2, deploymentRepository.findAll().size());
    }

    @Test
    void addMissingDeployment_createNewOne() {
        Assertions.assertEquals(0, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(2, deploymentRepository.findAll().size());
    }

    @Test
    void removeUnreferencedDeployments_directDeployment() {
        DeploymentEntity deploymentEntityOne = new DeploymentEntity();
        deploymentEntityOne.setAgentEntity(agentEntityOne);
        deploymentEntityOne.setPackageEntity(packageEntityOne);
        deploymentEntityOne.setDirectDeployment(true);
        deploymentRepository.save(deploymentEntityOne);
        DeploymentEntity deploymentEntityTwo = new DeploymentEntity();
        deploymentEntityTwo.setAgentEntity(agentEntityTwo);
        deploymentEntityTwo.setPackageEntity(packageEntityTwo);
        deploymentEntityTwo.setDirectDeployment(true);
        deploymentRepository.save(deploymentEntityTwo);

        groupEntityOne.removePackage(packageEntityOne);
        groupEntityOne = groupRepository.save(groupEntityOne);
        groupEntityTwo.removePackage(packageEntityTwo);
        groupEntityTwo = groupRepository.save(groupEntityTwo);

        Assertions.assertEquals(2, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(2, deploymentRepository.findAll().size());
    }

    @Test
    void removeUnreferencedDeployments_unreferencedGroupDeployments() {
        DeploymentEntity deploymentEntityOne = new DeploymentEntity();
        deploymentEntityOne.setAgentEntity(agentEntityOne);
        deploymentEntityOne.setPackageEntity(packageEntityOne);
        deploymentEntityOne.setDirectDeployment(false);
        deploymentRepository.save(deploymentEntityOne);
        DeploymentEntity deploymentEntityTwo = new DeploymentEntity();
        deploymentEntityTwo.setAgentEntity(agentEntityTwo);
        deploymentEntityTwo.setPackageEntity(packageEntityTwo);
        deploymentEntityTwo.setDirectDeployment(false);
        deploymentRepository.save(deploymentEntityTwo);

        groupEntityOne.removePackage(packageEntityOne);
        groupEntityOne = groupRepository.save(groupEntityOne);
        groupEntityTwo.removePackage(packageEntityTwo);
        groupEntityTwo = groupRepository.save(groupEntityTwo);

        Assertions.assertEquals(2, deploymentRepository.findAll().size());
        deploymentValidator.validateDeployments();
        Assertions.assertEquals(0, deploymentRepository.findAll().size());
    }
}
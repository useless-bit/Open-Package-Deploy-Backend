package org.codesystem.server.service.group;


import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.GroupRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.group.GroupCreateEmptyRequest;
import org.codesystem.server.request.group.GroupUpdateRequest;
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
class ManagementGroupServiceTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    AgentRepository agentRepository;
    @Autowired
    PackageRepository packageRepository;
    ManagementGroupService managementGroupService;

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
        agentEntityOne.setOperatingSystem(OperatingSystem.LINUX);
        agentEntityOne = agentRepository.save(agentEntityOne);
        agentEntityTwo = new AgentEntity();
        agentEntityTwo.setPublicKeyBase64("Agent Two Public Key");
        agentEntityTwo.setName("Agent Two");
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
        groupEntityOne.setOperatingSystem(OperatingSystem.LINUX);
        groupEntityTwo = groupRepository.save(groupEntityTwo);

        managementGroupService = new ManagementGroupService(groupRepository, agentRepository, packageRepository);
    }

    @AfterEach
    void tearDown() {
        groupRepository.deleteAll();
        agentRepository.deleteAll();
        packageRepository.deleteAll();
    }

    @Test
    void getAllGroups() {
        ResponseEntity responseEntity = managementGroupService.getAllGroups();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(groupRepository.findAll().get(0).getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("groups").get(0).toString()).getString("name"));
        Assertions.assertEquals(groupRepository.findAll().get(1).getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("groups").get(1).toString()).getString("name"));
    }

    @Test
    void getGroup() {
        ResponseEntity responseEntity = managementGroupService.getGroup("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.getGroup(groupEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Group 1", new JSONObject(responseEntity.getBody()).getString("name"));
        responseEntity = managementGroupService.getGroup(groupEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Group 2", new JSONObject(responseEntity.getBody()).getString("name"));
    }

    @Test
    void createEmptyGroup_invalid() {
        ResponseEntity responseEntity = managementGroupService.createEmptyGroup(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest(null, null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest("", null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest("   ", null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest(" new Group ", null, null));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest(" new Group ", null, OperatingSystem.UNKNOWN));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void createEmptyGroup_valid() {
        ResponseEntity responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest(" new Group ", null, OperatingSystem.LINUX));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        GroupEntity groupEntity = groupRepository.findFirstByUuid(new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("groupUUID"));
        Assertions.assertEquals("new Group", groupEntity.getName());
        Assertions.assertNull(groupEntity.getDescription());
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest(" new Group ", "   ", OperatingSystem.LINUX));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntity = groupRepository.findFirstByUuid(new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("groupUUID"));
        Assertions.assertEquals("new Group", groupEntity.getName());
        Assertions.assertNull(groupEntity.getDescription());
        responseEntity = managementGroupService.createEmptyGroup(new GroupCreateEmptyRequest(" new Group 1 ", " with description ", OperatingSystem.LINUX));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntity = groupRepository.findFirstByUuid(new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("groupUUID"));
        Assertions.assertEquals("new Group 1", groupEntity.getName());
        Assertions.assertEquals("with description", groupEntity.getDescription());
    }

    @Test
    void deleteGroup() {
        ResponseEntity responseEntity = managementGroupService.deleteGroup("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.deleteGroup(groupEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNull(groupRepository.findFirstByUuid(groupEntityOne.getUuid()));
        responseEntity = managementGroupService.deleteGroup(groupEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNull(groupRepository.findFirstByUuid(groupEntityTwo.getUuid()));
    }


    @Test
    void updateGroup_invalid() {
        ResponseEntity responseEntity = managementGroupService.updateGroup(null, null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.updateGroup("Invalid UUID", null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.updateGroup("Invalid UUID", new GroupUpdateRequest("new name", "new description"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void updateGroup_valid() {
        ResponseEntity responseEntity = managementGroupService.updateGroup(groupEntityOne.getUuid(), new GroupUpdateRequest(" new Group ", null));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals("new Group", groupEntity.getName());
        Assertions.assertNull(groupEntity.getDescription());
        responseEntity = managementGroupService.updateGroup(groupEntityOne.getUuid(), new GroupUpdateRequest(" New Group ", "   "));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntity = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals("New Group", groupEntity.getName());
        Assertions.assertNull(groupEntity.getDescription());
        responseEntity = managementGroupService.updateGroup(groupEntityOne.getUuid(), new GroupUpdateRequest(null, " with description "));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntity = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals("New Group", groupEntity.getName());
        Assertions.assertEquals("with description", groupEntity.getDescription());
        responseEntity = managementGroupService.updateGroup(groupEntityOne.getUuid(), new GroupUpdateRequest("   ", " With Description "));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntity = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals("New Group", groupEntity.getName());
        Assertions.assertEquals("With Description", groupEntity.getDescription());
        responseEntity = managementGroupService.updateGroup(groupEntityOne.getUuid(), new GroupUpdateRequest("   ", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntity = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals("New Group", groupEntity.getName());
        Assertions.assertNull(groupEntity.getDescription());
    }

    @Test
    void addAgent() {
        ResponseEntity responseEntity = managementGroupService.addAgent(groupEntityOne.getUuid(), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addAgent(groupEntityOne.getUuid(), "invalidUUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addAgent(null, agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addAgent("invalidUUID", agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addAgent(groupEntityOne.getUuid(), agentEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("OS mismatch", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        agentEntityTwo.setOperatingSystem(OperatingSystem.LINUX);
        agentEntityTwo = agentRepository.save(agentEntityTwo);
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getMembers().size());
        responseEntity = managementGroupService.addAgent(groupEntityOne.getUuid(), agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getMembers().size());
        responseEntity = managementGroupService.addAgent(groupEntityOne.getUuid(), agentEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(2, groupEntityOne.getMembers().size());
    }

    @Test
    void removeAgent() {
        ResponseEntity responseEntity = managementGroupService.removeAgent(groupEntityOne.getUuid(), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.removeAgent(groupEntityOne.getUuid(), "invalidUUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Agent not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.removeAgent(null, agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.removeAgent("invalidUUID", agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getMembers().size());
        responseEntity = managementGroupService.removeAgent(groupEntityOne.getUuid(), agentEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getMembers().size());
        responseEntity = managementGroupService.removeAgent(groupEntityOne.getUuid(), agentEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(0, groupEntityOne.getMembers().size());
    }

    @Test
    void addPackage() {
        ResponseEntity responseEntity = managementGroupService.addPackage(groupEntityOne.getUuid(), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addPackage(groupEntityOne.getUuid(), "invalidUUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addPackage(null, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addPackage("invalidUUID", packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.addPackage(groupEntityOne.getUuid(), packageEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("OS mismatch", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityTwo.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntityTwo = packageRepository.save(packageEntityTwo);
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getDeployedPackages().size());
        responseEntity = managementGroupService.addPackage(groupEntityOne.getUuid(), packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getDeployedPackages().size());
        responseEntity = managementGroupService.addPackage(groupEntityOne.getUuid(), packageEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(2, groupEntityOne.getDeployedPackages().size());
    }

    @Test
    void removePackage() {
        ResponseEntity responseEntity = managementGroupService.removePackage(groupEntityOne.getUuid(), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.removePackage(groupEntityOne.getUuid(), "invalidUUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.removePackage(null, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.removePackage("invalidUUID", packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getDeployedPackages().size());
        responseEntity = managementGroupService.removePackage(groupEntityOne.getUuid(), packageEntityTwo.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(1, groupEntityOne.getDeployedPackages().size());
        responseEntity = managementGroupService.removePackage(groupEntityOne.getUuid(), packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        groupEntityOne = groupRepository.findFirstByUuid(groupEntityOne.getUuid());
        Assertions.assertEquals(0, groupEntityOne.getDeployedPackages().size());
    }

    @Test
    void getMembers() {
        ResponseEntity responseEntity = managementGroupService.getMembers("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.getMembers(groupEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(new JSONObject(responseEntity.getBody()).getJSONArray("members").getJSONObject(0));
    }

    @Test
    void getPackages() {
        ResponseEntity responseEntity = managementGroupService.getPackages("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Group not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementGroupService.getPackages(groupEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(new JSONObject(responseEntity.getBody()).getJSONArray("packages").getJSONObject(0));
    }
}
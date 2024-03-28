package org.codesystem.server.service.agent.communication;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.agent.AgentEncryptedRequest;
import org.codesystem.server.response.agent.AgentEncryptedResponse;
import org.codesystem.server.utility.RequestUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import javax.crypto.KeyGenerator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;

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
    @Autowired
    PackageRepository packageRepository;
    RequestUtility requestUtility;
    ResourceLoader resourceLoader;
    AgentCommunicationService agentCommunicationService;
    Path packageFolder = Paths.get("/opt/OPD/Packages");


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
    void setUp() throws IOException {
        requestUtility = Mockito.mock(RequestUtility.class);
        resourceLoader = Mockito.mock(ResourceLoader.class);
        agentCommunicationService = new AgentCommunicationService(agentRepository, deploymentRepository, requestUtility, serverRepository, resourceLoader);
        deleteFolderWithContent();
    }

    @AfterEach
    void tearDown() throws IOException {
        deploymentRepository.deleteAll();
        packageRepository.deleteAll();
        serverRepository.deleteAll();
        agentRepository.deleteAll();
        deleteFolderWithContent();
    }

    private void deleteFolderWithContent() throws IOException {
        if (Files.exists(packageFolder)) {
            try (Stream<Path> pathStream = Files.walk(packageFolder)) {
                pathStream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
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
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
    }

    @Test
    void checkForUpdates_newDeploymentAvailable() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentRepository.save(deploymentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertTrue(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
    }

    @Test
    void checkForUpdates_newDeploymentPackageNotProcessed() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentRepository.save(deploymentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
        packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
    }

    @Test
    void checkForUpdates_failedDeploymentNotAvailable() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now());
        deploymentRepository.save(deploymentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
    }

    @Test
    void checkForUpdates_failedDeploymentAvailable() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverEntity = serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertTrue(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
    }

    @Test
    void checkForUpdates_nullValuesSystemInformation_AgentCheckForUpdateRequest() {
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
        JSONObject hardwareInfo = new JSONObject("""
                {
                    "operatingSystem": "LINUX",
                    "operatingSystemFamily": "Family",
                    "operatingSystemArchitecture": "64-bit",
                    "operatingSystemVersion": "Version",
                    "operatingSystemCodeName": "CodeName",
                    "operatingSystemBuildNumber": "14393",
                    "cpuName": "Intel Core i7",
                    "cpuArchitecture": "x64",
                    "cpuLogicalCores": "8",
                    "cpuPhysicalCores": "4",
                    "cpuSockets": "1",
                    "memory": "32 GB"
                }""");
        JSONObject jsonObject = new JSONObject().put("systemInformation", hardwareInfo).put("agentChecksum", "agentReportedChecksum");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        agentEntity = agentRepository.findFirstByPublicKeyBase64("agentPublicKey");
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNotNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
        Assertions.assertEquals(OperatingSystem.LINUX, agentEntity.getOperatingSystem());
        Assertions.assertEquals("Family", agentEntity.getOperatingSystemFamily());
        Assertions.assertEquals("64-bit", agentEntity.getOperatingSystemArchitecture());
        Assertions.assertEquals("Version", agentEntity.getOperatingSystemVersion());
        Assertions.assertEquals("CodeName", agentEntity.getOperatingSystemCodeName());
        Assertions.assertEquals("14393", agentEntity.getOperatingSystemBuildNumber());
        Assertions.assertEquals("Intel Core i7", agentEntity.getCpuName());
        Assertions.assertEquals("x64", agentEntity.getCpuArchitecture());
        Assertions.assertEquals("8", agentEntity.getCpuLogicalCores());
        Assertions.assertEquals("4", agentEntity.getCpuPhysicalCores());
        Assertions.assertEquals("1", agentEntity.getCpuSockets());
        Assertions.assertEquals("32 GB", agentEntity.getMemory());
    }

    @Test
    void checkForUpdates_valuesSystemInformation_AgentCheckForUpdateRequest() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setOperatingSystem(OperatingSystem.LINUX);
        agentEntity.setOperatingSystemFamily("Placeholder Family");
        agentEntity.setOperatingSystemArchitecture("Placeholder Architecture");
        agentEntity.setOperatingSystemVersion("0.0");
        agentEntity.setOperatingSystemCodeName("Placeholder Code Name");
        agentEntity.setOperatingSystemBuildNumber("00000");
        agentEntity.setCpuName("Placeholder CPU Name");
        agentEntity.setCpuArchitecture("Placeholder Architecture");
        agentEntity.setCpuLogicalCores("0");
        agentEntity.setCpuPhysicalCores("0");
        agentEntity.setCpuSockets("0");
        agentEntity.setMemory("0 MB");
        agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject hardwareInfo = new JSONObject("""
                {
                    "operatingSystem": "LINUX",
                    "operatingSystemFamily": "Family",
                    "operatingSystemArchitecture": "64-bit",
                    "operatingSystemVersion": "Version",
                    "operatingSystemCodeName": "CodeName",
                    "operatingSystemBuildNumber": "14393",
                    "cpuName": "Intel Core i7",
                    "cpuArchitecture": "x64",
                    "cpuLogicalCores": "8",
                    "cpuPhysicalCores": "4",
                    "cpuSockets": "1",
                    "memory": "32 GB"
                }""");
        JSONObject jsonObject = new JSONObject().put("systemInformation", hardwareInfo).put("agentChecksum", "agentReportedChecksum");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        agentEntity = agentRepository.findFirstByPublicKeyBase64("agentPublicKey");
        Assertions.assertEquals(100, jsonResponse.getInt("updateInterval"));
        Assertions.assertFalse(jsonResponse.getBoolean("deploymentAvailable"));
        Assertions.assertEquals("AgentChecksum", jsonResponse.getString("agentChecksum"));
        Assertions.assertNotNull(agentRepository.findFirstByPublicKeyBase64("agentPublicKey").getLastConnectionTime());
        Assertions.assertEquals(OperatingSystem.LINUX, agentEntity.getOperatingSystem());
        Assertions.assertEquals("Family", agentEntity.getOperatingSystemFamily());
        Assertions.assertEquals("64-bit", agentEntity.getOperatingSystemArchitecture());
        Assertions.assertEquals("Version", agentEntity.getOperatingSystemVersion());
        Assertions.assertEquals("CodeName", agentEntity.getOperatingSystemCodeName());
        Assertions.assertEquals("14393", agentEntity.getOperatingSystemBuildNumber());
        Assertions.assertEquals("Intel Core i7", agentEntity.getCpuName());
        Assertions.assertEquals("x64", agentEntity.getCpuArchitecture());
        Assertions.assertEquals("8", agentEntity.getCpuLogicalCores());
        Assertions.assertEquals("4", agentEntity.getCpuPhysicalCores());
        Assertions.assertEquals("1", agentEntity.getCpuSockets());
        Assertions.assertEquals("32 GB", agentEntity.getMemory());
    }

    @Test
    void checkForUpdates_updateFromUnknownToLinux() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setOperatingSystem(OperatingSystem.UNKNOWN);
        agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject hardwareInfo = new JSONObject("""
                {
                    "operatingSystem": "LINUX"
                }""");
        JSONObject jsonObject = new JSONObject().put("systemInformation", hardwareInfo).put("agentChecksum", "agentReportedChecksum");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        agentEntity = agentRepository.findFirstByPublicKeyBase64("agentPublicKey");
        Assertions.assertEquals(OperatingSystem.LINUX, agentEntity.getOperatingSystem());
    }

    @Test
    void checkForUpdates_updateFromUnknownToUnknown() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setOperatingSystem(OperatingSystem.UNKNOWN);
        agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject hardwareInfo = new JSONObject("""
                {
                    "operatingSystem": "INVALID"
                }""");
        JSONObject jsonObject = new JSONObject().put("systemInformation", hardwareInfo).put("agentChecksum", "agentReportedChecksum");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Cannot set 'UNKNOWN' OperatingSystem", new JSONObject(responseEntity.getBody()).getString("message"));
        agentEntity = agentRepository.findFirstByPublicKeyBase64("agentPublicKey");
        Assertions.assertEquals(OperatingSystem.UNKNOWN, agentEntity.getOperatingSystem());
    }

    @Test
    void checkForUpdates_updateFromLinuxToUnknown() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity.setOperatingSystem(OperatingSystem.UNKNOWN);
        agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        JSONObject hardwareInfo = new JSONObject("""
                {
                    "operatingSystem": "INVALID"
                }""");
        JSONObject jsonObject = new JSONObject().put("systemInformation", hardwareInfo).put("agentChecksum", "agentReportedChecksum");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.checkForUpdates(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Cannot set 'UNKNOWN' OperatingSystem", new JSONObject(responseEntity.getBody()).getString("message"));
        agentEntity = agentRepository.findFirstByPublicKeyBase64("agentPublicKey");
        Assertions.assertEquals(OperatingSystem.UNKNOWN, agentEntity.getOperatingSystem());
    }

    @Test
    void getAgent_invalidRequest() {
        ResponseEntity responseEntity = agentCommunicationService.getAgent(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(null);
        responseEntity = agentCommunicationService.getAgent(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        JSONObject jsonObject = new JSONObject();
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        responseEntity = agentCommunicationService.getAgent(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getAgent_invalidAgent() {
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getAgent(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getAgent_FileNotPresent() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentRepository.save(agentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.getAgent(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    void getAgent_valid() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentRepository.save(agentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        Mockito.when(resourceLoader.getResource(Mockito.any())).thenReturn(new ClassPathResource("Test-File"));
        ResponseEntity responseEntity = agentCommunicationService.getAgent(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        byte[] response = (byte[]) responseEntity.getBody();
        Assertions.assertArrayEquals("Test File Content".getBytes(StandardCharsets.UTF_8), response);
    }

    @Test
    void getPackage_invalidRequest() {
        ResponseEntity responseEntity = agentCommunicationService.getPackage(null, null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(null);
        responseEntity = agentCommunicationService.getPackage(null, null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        JSONObject jsonObject = new JSONObject();
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        responseEntity = agentCommunicationService.getPackage(null, null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getPackage_invalidAgent() {
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getPackage_invalidDeployment() {
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
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getPackage_deploymentAlreadyCompleted() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(true);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getPackage_deploymentNotProcessed() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
        packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getPackage_deploymentTimestampNotNullInvalidTimestamp() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverEntity.setAgentInstallRetryInterval(60000);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now());
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(600, ChronoUnit.SECONDS));
        deploymentRepository.save(deploymentEntity);
        responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void getPackage_deploymentTimestampNull() throws IOException {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        packageFolder.toFile().mkdirs();
        new FileOutputStream(packageFolder.toString() + "/" + packageEntity.getUuid()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(null);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        FileSystemResource response = (FileSystemResource) responseEntity.getBody();
        Assertions.assertArrayEquals("Test Content".getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray());
    }

    @Test
    void getPackage_deploymentTimestampOlderThanLimit() throws IOException {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        packageFolder.toFile().mkdirs();
        new FileOutputStream(packageFolder.toString() + "/" + packageEntity.getUuid()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackage(new AgentEncryptedRequest("agentPublicKey", ""), deploymentEntity.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        FileSystemResource response = (FileSystemResource) responseEntity.getBody();
        Assertions.assertArrayEquals("Test Content".getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray());
    }

    @Test
    void getPackageDetails_invalidRequest() {
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(null);
        responseEntity = agentCommunicationService.getPackageDetails(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        JSONObject jsonObject = new JSONObject();
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        responseEntity = agentCommunicationService.getPackageDetails(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void getPackageDetails_invalidAgent() {
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Key", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void getPackageDetails_noDeployment() {
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
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void getPackageDetails_deploymentAlreadyCompleted() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(true);
        deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void getPackageDetails_deploymentNotProcessed() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
        packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
        packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void getPackageDetails_deploymentTimestampNotNullInvalidTimestamp() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverEntity.setAgentInstallRetryInterval(60000);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now());
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(600, ChronoUnit.SECONDS));
        deploymentRepository.save(deploymentEntity);
        responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("No Deployment available", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void getPackageDetailsDetails_deploymentTimestampNull() throws NoSuchAlgorithmException {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setChecksumEncrypted("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        packageEntity.setEncryptionToken(keyGenerator.generateKey());
        packageEntity.setInitializationVector("IV".getBytes(StandardCharsets.UTF_8));
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(null);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(deploymentEntity.getUuid(), jsonResponse.getString("deploymentUUID"));
        Assertions.assertEquals("CheckSum", jsonResponse.getString("checksumPlaintext"));
    }

    @Test
    void getPackageDetailsDetails_deploymentTimestampOlderThanLimit() throws NoSuchAlgorithmException {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setChecksumEncrypted("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        packageEntity.setEncryptionToken(keyGenerator.generateKey());
        packageEntity.setInitializationVector("IV".getBytes(StandardCharsets.UTF_8));
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.getPackageDetails(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONObject jsonResponse = new JSONObject(new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(deploymentEntity.getUuid(), jsonResponse.getString("deploymentUUID"));
        Assertions.assertEquals("CheckSum", jsonResponse.getString("checksumPlaintext"));
    }

    @Test
    void sendDeploymentResult_invalidRequest() {
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(null);
        responseEntity = agentCommunicationService.sendDeploymentResult(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
        JSONObject jsonObject = new JSONObject();
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        responseEntity = agentCommunicationService.sendDeploymentResult(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_invalidAgent() {
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "agentPublicKey");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Key", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_invalidAgentDeploymentResultRequest() {
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
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_noDeployment() {
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
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", "invalidDeployment");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_deploymentAlreadyCompleted() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(true);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_deploymentNotProcessed() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageEntity = packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
        packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
        packageRepository.save(packageEntity);
        responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_deploymentTimestampNotNullInvalidTimestamp() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverEntity.setAgentInstallRetryInterval(60000);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now());
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(600, ChronoUnit.SECONDS));
        deploymentRepository.save(deploymentEntity);
        responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Deployment", new JSONObject(responseEntity.getBody()).getString("message"));
    }

    @Test
    void sendDeploymentResult_deploymentTimestampNull() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(null);
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntity = deploymentRepository.findFirstByUuid(deploymentEntity.getUuid());
        Assertions.assertFalse(deploymentEntity.isDeployed());
        Assertions.assertNull(deploymentEntity.getReturnValue());
    }

    @Test
    void sendDeploymentResult_deploymentTimestampOlderThanLimit() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid());
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntity = deploymentRepository.findFirstByUuid(deploymentEntity.getUuid());
        Assertions.assertFalse(deploymentEntity.isDeployed());
        Assertions.assertNull(deploymentEntity.getReturnValue());
    }

    @Test
    void sendDeploymentResult_resultCodeErrorNoReference() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid()).put("resultCode", "AGENT-DEPLOYMENT-ERROR: Error");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntity = deploymentRepository.findFirstByUuid(deploymentEntity.getUuid());
        Assertions.assertFalse(deploymentEntity.isDeployed());
        Assertions.assertEquals("AGENT-DEPLOYMENT-ERROR: Error", deploymentEntity.getReturnValue());
    }

    @Test
    void sendDeploymentResult_resultCodeNoReference() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid()).put("resultCode", "code");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntity = deploymentRepository.findFirstByUuid(deploymentEntity.getUuid());
        Assertions.assertTrue(deploymentEntity.isDeployed());
        Assertions.assertEquals("code", deploymentEntity.getReturnValue());
    }

    @Test
    void sendDeploymentResult_resultCodeWrongReference() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setExpectedReturnValue("expected Value");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid()).put("resultCode", "code");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntity = deploymentRepository.findFirstByUuid(deploymentEntity.getUuid());
        Assertions.assertFalse(deploymentEntity.isDeployed());
        Assertions.assertEquals("code", deploymentEntity.getReturnValue());
    }

    @Test
    void sendDeploymentResult_resultCodeCorrectReference() {
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setChecksumPlaintext("CheckSum");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setName("Package");
        packageEntity.setExpectedReturnValue("expected Value");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntity = packageRepository.save(packageEntity);
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName("Test-Agent");
        agentEntity.setPublicKeyBase64("agentPublicKey");
        agentEntity = agentRepository.save(agentEntity);
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentChecksum("AgentChecksum");
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setPrivateKeyBase64("Private Key");
        serverEntity.setPublicKeyBase64("Public Key");
        serverEntity.setAgentUpdateInterval(100);
        serverRepository.save(serverEntity);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setDeployed(false);
        deploymentEntity.setLastDeploymentTimestamp(Instant.now().minus(serverEntity.getAgentInstallRetryInterval(), ChronoUnit.SECONDS));
        deploymentEntity = deploymentRepository.save(deploymentEntity);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", deploymentEntity.getUuid()).put("resultCode", "expected Value");
        Mockito.when(requestUtility.validateRequest(Mockito.any())).thenReturn(jsonObject);
        Mockito.when(requestUtility.generateAgentEncryptedResponse(Mockito.any(), Mockito.any())).then(invocationOnMock -> new AgentEncryptedResponse(invocationOnMock.getArgument(0).toString()));
        ResponseEntity responseEntity = agentCommunicationService.sendDeploymentResult(new AgentEncryptedRequest("agentPublicKey", ""));
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        deploymentEntity = deploymentRepository.findFirstByUuid(deploymentEntity.getUuid());
        Assertions.assertTrue(deploymentEntity.isDeployed());
        Assertions.assertEquals("expected Value", deploymentEntity.getReturnValue());
    }

}
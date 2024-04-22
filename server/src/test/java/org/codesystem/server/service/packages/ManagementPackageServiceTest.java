package org.codesystem.server.service.packages;

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
import org.codesystem.server.request.packages.PackageAddNewRequest;
import org.codesystem.server.request.packages.PackageUpdateContentRequest;
import org.codesystem.server.request.packages.PackageUpdateRequest;
import org.codesystem.server.service.server.LogService;
import org.codesystem.server.utility.CryptoUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManagementPackageServiceTest {

    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    DeploymentRepository deploymentRepository;
    @Autowired
    AgentRepository agentRepository;
    CryptoUtility cryptoUtility;
    LogService logService;
    PackageEntity packageEntityOne;
    PackageEntity packageEntityTwo;
    ManagementPackageService managementPackageService;
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

        cryptoUtility = Mockito.mock(CryptoUtility.class);
        logService = Mockito.mock(LogService.class);

        managementPackageService = new ManagementPackageService(packageRepository, cryptoUtility, deploymentRepository, logService);
        deleteFolderWithContent();
    }

    @AfterEach
    void tearDown() throws IOException {
        deploymentRepository.deleteAll();
        packageRepository.deleteAll();
        deleteFolderWithContent();
    }

    @Test
    void getAllPackages() {
        ResponseEntity responseEntity = managementPackageService.getAllPackages();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(packageRepository.findAll().get(0).getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("packages").get(0).toString()).getString("name"));
        Assertions.assertEquals(packageRepository.findAll().get(1).getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("packages").get(1).toString()).getString("name"));
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
    void getPackage() {
        ResponseEntity responseEntity = managementPackageService.getPackage(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.getPackage("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        responseEntity = managementPackageService.getPackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Package One", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("name"));
    }

    @Test
    void addNewPackage_invalidRequest() {
        ResponseEntity responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest(null, null, OperatingSystem.UNKNOWN, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("", null, OperatingSystem.UNKNOWN, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("   ", null, OperatingSystem.UNKNOWN, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, null, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, OperatingSystem.UNKNOWN, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void addNewPackage_invalidMultiPartFile() {
        ResponseEntity responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, OperatingSystem.LINUX, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        byte[] multiPartFileContent = null;
        MultipartFile multipartFile = new MockMultipartFile("FileName", "FileName", null, multiPartFileContent);
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, OperatingSystem.LINUX, null), multipartFile);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", null, multiPartFileContent);
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, OperatingSystem.LINUX, null), multipartFile);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", "wrong content-type", multiPartFileContent);
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, OperatingSystem.LINUX, null), multipartFile);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void addNewPackage_invalidChecksum() {
        byte[] multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        MultipartFile multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Test CheckSum");
        ResponseEntity responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", null, OperatingSystem.LINUX, null), multipartFile);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Checksum mismatch", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Test CheckSum");
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest("New Package", "invalid CheckSum", OperatingSystem.LINUX, null), multipartFile);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Checksum mismatch", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void addNewPackage_valid() {
        byte[] multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        MultipartFile multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Test CheckSum");
        ResponseEntity responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest(" New Package ", "Test CheckSum", OperatingSystem.LINUX, null), multipartFile);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        PackageEntity packageEntity = packageRepository.findFirstByPackageStatusInternal(PackageStatusInternal.UPLOADED);
        Assertions.assertEquals("New Package", packageEntity.getName());
        Assertions.assertNull(packageEntity.getExpectedReturnValue());
        Assertions.assertEquals("Test CheckSum", packageEntity.getChecksumPlaintext());
        Assertions.assertNull(packageEntity.getChecksumEncrypted());
        Assertions.assertEquals(OperatingSystem.LINUX, packageEntity.getTargetOperatingSystem());

        packageRepository.deleteAll();

        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Test CheckSum");
        responseEntity = managementPackageService.addNewNewPackage(new PackageAddNewRequest(" New Package ", "Test CheckSum", OperatingSystem.LINUX, "Return Value"), multipartFile);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntity = packageRepository.findFirstByPackageStatusInternal(PackageStatusInternal.UPLOADED);
        Assertions.assertEquals("New Package", packageEntity.getName());
        Assertions.assertEquals("Return Value", packageEntity.getExpectedReturnValue());
        Assertions.assertEquals("Test CheckSum", packageEntity.getChecksumPlaintext());
        Assertions.assertNull(packageEntity.getChecksumEncrypted());
        Assertions.assertEquals(OperatingSystem.LINUX, packageEntity.getTargetOperatingSystem());
    }

    @Test
    void updatePackage_invalid() {
        ResponseEntity responseEntity = managementPackageService.updatePackage(new PackageUpdateRequest("", ""), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.updatePackage(new PackageUpdateRequest("", ""), "invlid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.updatePackage(null, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void updatePackage_valid() {
        ResponseEntity responseEntity = managementPackageService.updatePackage(new PackageUpdateRequest(null, "-1"), packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertEquals("-1", packageEntityOne.getExpectedReturnValue());
        Assertions.assertEquals("Package One", packageEntityOne.getName());

        responseEntity = managementPackageService.updatePackage(new PackageUpdateRequest("New Package One", null), packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertNull(packageEntityOne.getExpectedReturnValue());
        Assertions.assertEquals("New Package One", packageEntityOne.getName());

        responseEntity = managementPackageService.updatePackage(new PackageUpdateRequest("New Package One", null), packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertNull(packageEntityOne.getExpectedReturnValue());
        Assertions.assertEquals("New Package One", packageEntityOne.getName());

        responseEntity = managementPackageService.updatePackage(new PackageUpdateRequest(" New Package One ", "new Return Value"), packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertEquals("new Return Value", packageEntityOne.getExpectedReturnValue());
        Assertions.assertEquals("New Package One", packageEntityOne.getName());
    }

    @Test
    void deletePackage() {
        ResponseEntity responseEntity = managementPackageService.deletePackage(null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.deletePackage("invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementPackageService.deletePackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Cannot delete package during processing", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementPackageService.deletePackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package already marked for deletion", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementPackageService.deletePackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertEquals(PackageStatusInternal.MARKED_AS_DELETED, packageEntityOne.getPackageStatusInternal());

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementPackageService.deletePackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertEquals(PackageStatusInternal.MARKED_AS_DELETED, packageEntityOne.getPackageStatusInternal());

        packageEntityOne.setPackageStatusInternal(PackageStatusInternal.ERROR);
        packageEntityOne = packageRepository.save(packageEntityOne);
        responseEntity = managementPackageService.deletePackage(packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        packageEntityOne = packageRepository.findFirstByUuid(packageEntityOne.getUuid());
        Assertions.assertEquals(PackageStatusInternal.MARKED_AS_DELETED, packageEntityOne.getPackageStatusInternal());
    }

    @Test
    void updatePackageContent_invalidPackage() {
        ResponseEntity responseEntity = managementPackageService.updatePackageContent(null, null, null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest(""), null, "Invalid UUID");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Package not found", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void updatePackageContent_invalidMultiPartFile() {
        ResponseEntity responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest(""), null, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

        byte[] multiPartFileContent = null;
        MultipartFile multipartFile = new MockMultipartFile("FileName", "FileName", null, multiPartFileContent);
        responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest(""), multipartFile, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", null, multiPartFileContent);
        responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest(""), multipartFile, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", "wrong content-type", multiPartFileContent);
        responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest(""), multipartFile, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid zip-file", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void updatePackageContent_invalidChecksum() {
        byte[] multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        MultipartFile multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Test CheckSum");
        ResponseEntity responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest(null), multipartFile, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Checksum mismatch", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Test CheckSum");
        responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest("Invalid Checksum"), multipartFile, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Checksum mismatch", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
    }

    @Test
    void updatePackageContent_valid() {
        AgentEntity agentEntityOne = new AgentEntity();
        agentEntityOne.setName("Agent One");
        agentEntityOne.setPublicKeyBase64("Agent One Public Key");
        agentEntityOne = agentRepository.save(agentEntityOne);

        DeploymentEntity deploymentEntityOne = new DeploymentEntity();
        deploymentEntityOne.setPackageEntity(packageEntityOne);
        deploymentEntityOne.setAgentEntity(agentEntityOne);
        deploymentEntityOne.setDeployed(true);
        deploymentEntityOne.setLastDeploymentTimestamp(Instant.now());
        deploymentEntityOne = deploymentRepository.save(deploymentEntityOne);

        byte[] multiPartFileContent = "Test Content".getBytes(StandardCharsets.UTF_8);
        MultipartFile multipartFile = new MockMultipartFile("FileName", "FileName", "application/zip", multiPartFileContent);
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("New CheckSum");
        ResponseEntity responseEntity = managementPackageService.updatePackageContent(new PackageUpdateContentRequest("New CheckSum"), multipartFile, packageEntityOne.getUuid());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        PackageEntity packageEntity = packageRepository.findFirstByPackageStatusInternal(PackageStatusInternal.UPLOADED);
        deploymentEntityOne = deploymentRepository.findFirstByUuid(deploymentEntityOne.getUuid());
        Assertions.assertFalse(deploymentEntityOne.isDeployed());
        Assertions.assertNull(deploymentEntityOne.getLastDeploymentTimestamp());
        Assertions.assertEquals("New CheckSum", packageEntity.getChecksumPlaintext());
    }

}
package org.codesystem.server.service.packages;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.packages.AddNewPackageRequest;
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

import java.util.Objects;

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
    CryptoUtility cryptoUtility;
    PackageEntity packageEntityOne;
    PackageEntity packageEntityTwo;
    ManagementPackageService managementPackageService;

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

        cryptoUtility = Mockito.mock(CryptoUtility.class);

        managementPackageService = new ManagementPackageService(packageRepository, cryptoUtility, deploymentRepository);
    }

    @AfterEach
    void tearDown() {
        deploymentRepository.deleteAll();
        packageRepository.deleteAll();
    }

    @Test
    void getAllPackages() {
        ResponseEntity responseEntity = managementPackageService.getAllPackages();
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(packageRepository.findAll().get(0).getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("packages").get(0).toString()).getString("name"));
        Assertions.assertEquals(packageRepository.findAll().get(1).getName(), new JSONObject(new JSONObject(responseEntity.getBody()).getJSONArray("packages").get(1).toString()).getString("name"));
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
        ResponseEntity responseEntity = managementPackageService.addNewNewPackage(new AddNewPackageRequest(), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.addNewNewPackage(new AddNewPackageRequest(null, null, null, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));
        responseEntity = managementPackageService.addNewNewPackage(new AddNewPackageRequest(null, null, null, null), null);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals("Invalid Request", new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getString("message"));

    }

}
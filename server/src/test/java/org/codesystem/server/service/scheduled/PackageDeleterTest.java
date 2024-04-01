package org.codesystem.server.service.scheduled;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.PackageRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PackageDeleterTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    PackageRepository packageRepository;
    PackageDeleter packageDeleter;
    PackageEntity packageEntity;
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
        packageEntity = new PackageEntity();
        packageEntity.setName("Package");
        packageEntity.setTargetOperatingSystem(OperatingSystem.LINUX);
        packageEntity.setChecksumPlaintext("PlainText Checksum");
        packageEntity = packageRepository.save(packageEntity);
        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntity = packageRepository.save(packageEntity);

        packageDeleter = new PackageDeleter(packageRepository);
        deleteFolderWithContent();
    }

    @AfterEach
    void tearDown() throws IOException {
        packageRepository.deleteAll();
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
    void deletePackage() throws IOException {
        Files.createDirectory(packageFolder);
        Path filePath = Paths.get(packageFolder.toString() + File.separator + packageEntity.getUuid());
        new FileOutputStream(filePath.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));

        packageDeleter.deletePackage();

        Assertions.assertEquals("Package", packageRepository.findFirstByUuid(packageEntity.getUuid()).getName());
        Assertions.assertTrue(filePath.toFile().exists());

        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        packageRepository.save(packageEntity);

        packageDeleter.deletePackage();

        Assertions.assertNull(packageRepository.findFirstByUuid(packageEntity.getUuid()));
        Assertions.assertFalse(filePath.toFile().exists());
    }
}
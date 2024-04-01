package org.codesystem.server.service.scheduled;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.utility.CryptoUtility;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
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
class PackageEncryptorTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    PackageRepository packageRepository;
    PackageEncryptor packageEncryptor;
    CryptoUtility cryptoUtility;
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

        cryptoUtility = Mockito.mock(CryptoUtility.class);
        packageEncryptor = new PackageEncryptor(packageRepository, cryptoUtility);
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
    void encryptPackage_invalidPlaintextFile() throws IOException {
        // no file
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR_FILE_NOT_FOUND, packageEntity.getPackageStatusInternal());

        // folder instead of file
        Files.createDirectories(Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext"));
        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageRepository.save(packageEntity);
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR_FILE_NOT_FOUND, packageEntity.getPackageStatusInternal());
    }

    @Test
    void encryptPackage_invalidPlaintextChecksum() throws IOException {
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("Wrong Checksum");
        Files.createDirectory(packageFolder);
        Path plaintextFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext");
        Path decryptedFileTest = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_temp-encrypted-test");
        new FileOutputStream(plaintextFile.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR_CHECKSUM_MISMATCH, packageEntity.getPackageStatusInternal());
        Assertions.assertFalse(plaintextFile.toFile().exists());
        Assertions.assertFalse(decryptedFileTest.toFile().exists());
    }

    @Test
    void encryptPackage_failureEncryption() throws IOException {
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("PlainText Checksum");
        Mockito.when(cryptoUtility.encryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);
        Files.createDirectory(packageFolder);
        Path plaintextFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext");
        Path decryptedFileTest = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_temp-encrypted-test");
        new FileOutputStream(plaintextFile.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR_ENCRYPTION, packageEntity.getPackageStatusInternal());
        Assertions.assertFalse(plaintextFile.toFile().exists());
        Assertions.assertFalse(decryptedFileTest.toFile().exists());
    }

    @Test
    void encryptPackage_encryptedChecksumCalculationException() throws IOException {
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("PlainText Checksum");
        Mockito.when(cryptoUtility.encryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Files.createDirectory(packageFolder);
        Path plaintextFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext");
        Path decryptedFileTest = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_temp-encrypted-test");
        new FileOutputStream(plaintextFile.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR, packageEntity.getPackageStatusInternal());
        Assertions.assertFalse(plaintextFile.toFile().exists());
        Assertions.assertFalse(decryptedFileTest.toFile().exists());
    }

    @Test
    void encryptPackage_failureDecryption() throws IOException {
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("PlainText Checksum");
        Mockito.when(cryptoUtility.encryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);
        Files.createDirectory(packageFolder);
        Path plaintextFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext");
        Path encryptedFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid());
        Path decryptedFileTest = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_temp-encrypted-test");
        new FileOutputStream(plaintextFile.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        new FileOutputStream(encryptedFile.toFile()).write("Test Content Encrypted".getBytes(StandardCharsets.UTF_8));
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR_DECRYPTION, packageEntity.getPackageStatusInternal());
        Assertions.assertFalse(plaintextFile.toFile().exists());
        Assertions.assertFalse(decryptedFileTest.toFile().exists());
    }

    @Test
    void encryptPackage_compareChecksumFailureException() throws IOException {
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("PlainText Checksum");
        Mockito.when(cryptoUtility.encryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Files.createDirectory(packageFolder);
        Path plaintextFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext");
        Path encryptedFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid());
        Path decryptedFileTest = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_temp-encrypted-test");
        new FileOutputStream(plaintextFile.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        new FileOutputStream(encryptedFile.toFile()).write("Test Content Encrypted".getBytes(StandardCharsets.UTF_8));
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.ERROR_CHECKSUM_MISMATCH, packageEntity.getPackageStatusInternal());
        Assertions.assertFalse(plaintextFile.toFile().exists());
        Assertions.assertFalse(decryptedFileTest.toFile().exists());
    }

    @Test
    void encryptPackage_valid() throws IOException {
        Mockito.when(cryptoUtility.calculateChecksum(Mockito.any())).thenReturn("PlainText Checksum");
        Mockito.when(cryptoUtility.encryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Files.createDirectory(packageFolder);
        Path plaintextFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_plaintext");
        Path encryptedFile = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid());
        Path decryptedFileTest = Paths.get(packageFolder.toString() + "/" + packageEntity.getUuid() + "_temp-encrypted-test");
        new FileOutputStream(plaintextFile.toFile()).write("Test Content".getBytes(StandardCharsets.UTF_8));
        new FileOutputStream(encryptedFile.toFile()).write("Test Content Encrypted".getBytes(StandardCharsets.UTF_8));
        new FileOutputStream(decryptedFileTest.toFile()).write("Test Content Decrypted".getBytes(StandardCharsets.UTF_8));
        packageEncryptor.encryptPackage();
        packageEntity = packageRepository.findFirstByUuid(packageEntity.getUuid());
        Assertions.assertEquals(PackageStatusInternal.PROCESSED, packageEntity.getPackageStatusInternal());
        Assertions.assertFalse(plaintextFile.toFile().exists());
        Assertions.assertFalse(decryptedFileTest.toFile().exists());
    }

}
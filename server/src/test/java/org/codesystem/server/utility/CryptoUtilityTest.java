package org.codesystem.server.utility;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.exception.TestSystemExitException;
import org.codesystem.server.repository.ServerRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CryptoUtilityTest {
    private static MariaDB4jSpringService DB;

    CryptoUtility cryptoUtility;
    @MockBean
    ServerInitialization serverInitialization;
    @MockBean
    SecurityConfiguration securityConfiguration;
    @Autowired
    ServerRepository serverRepository;

    PrivateKey serverPrivateKey;
    PublicKey serverPublicKey;
    PrivateKey agentPrivateKey;
    PublicKey agentPublicKey;
    File encryptedFile = Path.of("encryptedFile").toFile();
    File plaintextFile = Path.of("plaintextFile").toFile();
    Path pathForDecryptedFile = Path.of("decryptedFile");
    IESParameterSpec iesParamSpec = new IESParameterSpec(
            /* derivation = */ null,
            /* encoding = */ null,
            /* macKeySize = */ 1024,
            /* cipherKeySize = */ 1024,
            /* nonce = */ null,
            /* usePointCompression = */ false);


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
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        MockitoAnnotations.openMocks(this);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        serverPrivateKey = keyPair.getPrivate();
        serverPublicKey = keyPair.getPublic();
        keyPair = keyPairGenerator.generateKeyPair();
        agentPrivateKey = keyPair.getPrivate();
        agentPublicKey = keyPair.getPublic();

        SystemExitUtility systemExitUtility = Mockito.mock(SystemExitUtility.class);
        Mockito.doThrow(TestSystemExitException.class).when(systemExitUtility).exit(Mockito.anyInt());

        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPrivateKeyBase64(Base64.getEncoder().encodeToString(serverPrivateKey.getEncoded()));
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        serverEntity.setAgentUpdateInterval(60);
        serverEntity.setAgentInstallRetryInterval(600);
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setAgentChecksum("Checksum");
        serverRepository.save(serverEntity);
        cryptoUtility = new CryptoUtility(serverRepository);

        encryptedFile.delete();
        pathForDecryptedFile.toFile().delete();
        plaintextFile.delete();
    }

    @AfterEach
    void tearDown() {
        serverRepository.deleteAll();
        encryptedFile.delete();
        pathForDecryptedFile.toFile().delete();
        plaintextFile.delete();
    }

    @Test
    void constructor_invalid() {
        serverRepository.deleteAll();
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPrivateKeyBase64("");
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        serverEntity.setAgentUpdateInterval(60);
        serverEntity.setAgentInstallRetryInterval(600);
        serverEntity.setAgentRegistrationToken("Registration Token");
        serverEntity.setAgentChecksum("Checksum");
        serverRepository.save(serverEntity);
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility = new CryptoUtility(serverRepository));
    }

    @Test
    void decryptECC_invalid() {
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.decryptECC(null));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.decryptECC("".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void decryptECC_valid() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey, iesParamSpec);
        byte[] encryptedMessage = cipher.doFinal("Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("Test Message", cryptoUtility.decryptECC(encryptedMessage));
    }

    @Test
    void encryptECC_invalid() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC(null, null));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC("Test Message".getBytes(StandardCharsets.UTF_8), null));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC("Test Message".getBytes(StandardCharsets.UTF_8), new AgentEntity()));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC(null, agentEntity));
    }

    @Test
    void encryptECC_valid() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        byte[] encryptedMessage = cryptoUtility.encryptECC("Test Message".getBytes(StandardCharsets.UTF_8), agentEntity);
        Cipher cipher = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, agentPrivateKey, iesParamSpec);
        Assertions.assertArrayEquals("Test Message".getBytes(StandardCharsets.UTF_8), cipher.doFinal(encryptedMessage));
    }

    @Test
    void createSignatureECC_invalid() {
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.createSignatureECC(null));
    }

    @Test
    void createSignatureECC_valid() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        byte[] messageSignature = cryptoUtility.createSignatureECC("Test Message");
        Signature signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
        signature.initVerify(serverPublicKey);
        signature.update("Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(signature.verify(messageSignature));
        signature.update("Wrong Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertFalse(signature.verify(messageSignature));
    }

    @Test
    void verifySignatureECC_invalid() {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.verifySignatureECC(null, null, null));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.verifySignatureECC("", "", new AgentEntity()));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.verifySignatureECC("Message", "Signature", agentEntity));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.verifySignatureECC(null, "Signature", agentEntity));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.verifySignatureECC("Message", null, agentEntity));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.verifySignatureECC("Message", "Signature", null));
    }

    @Test
    void verifySignatureECC_valid() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        Signature signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
        signature.initSign(agentPrivateKey);
        signature.update("Test Message".getBytes(StandardCharsets.UTF_8));
        byte[] messageSignature = signature.sign();
        Assertions.assertTrue(cryptoUtility.verifySignatureECC("Test Message", Base64.getEncoder().encodeToString(messageSignature), agentEntity));
        Assertions.assertFalse(cryptoUtility.verifySignatureECC("Wrong Test Message", Base64.getEncoder().encodeToString(messageSignature), agentEntity));
    }

    @Test
    void encryptFile_invalid() {
        PackageEntity packageEntity = new PackageEntity();
        Assertions.assertFalse(cryptoUtility.encryptFile(packageEntity, plaintextFile, encryptedFile.toPath()));
        Assertions.assertFalse(cryptoUtility.encryptFile(packageEntity, null, encryptedFile.toPath()));
        Assertions.assertFalse(cryptoUtility.encryptFile(packageEntity, plaintextFile, null));
    }

    @Test
    void encryptFile_valid() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        PackageEntity packageEntity = new PackageEntity();
        new FileOutputStream(plaintextFile).write("Test Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(cryptoUtility.encryptFile(packageEntity, plaintextFile, encryptedFile.toPath()));
        Assertions.assertTrue(encryptedFile.exists());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        try (
                FileInputStream fileInputStream = new FileInputStream(encryptedFile);
                CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
                FileOutputStream fileOutputStream = new FileOutputStream(pathForDecryptedFile.toString())
        ) {
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, packageEntity.getInitializationVector());
            cipher.init(Cipher.DECRYPT_MODE, packageEntity.getEncryptionToken(), gcmParameterSpec);
            byte[] inputStreamByte = cipherInputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                fileOutputStream.write(inputStreamByte);
                inputStreamByte = cipherInputStream.readNBytes(1024);
            }
        }
        Assertions.assertArrayEquals("Test Content".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(pathForDecryptedFile));
        Assertions.assertNotNull(packageEntity.getEncryptionToken());
        Assertions.assertNotNull(packageEntity.getInitializationVector());
    }


    @Test
    void decryptFile_invalid() {
        PackageEntity packageEntity = new PackageEntity();
        Assertions.assertFalse(cryptoUtility.decryptFile(packageEntity, encryptedFile, pathForDecryptedFile));
        Assertions.assertFalse(cryptoUtility.decryptFile(packageEntity, null, pathForDecryptedFile));
        Assertions.assertFalse(cryptoUtility.decryptFile(packageEntity, encryptedFile, null));
    }

    @Test
    void decryptFile_valid() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        PackageEntity packageEntity = new PackageEntity();
        new FileOutputStream(plaintextFile).write("Test Content".getBytes(StandardCharsets.UTF_8));
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        packageEntity.setEncryptionToken(keyGenerator.generateKey());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, packageEntity.getEncryptionToken());
        try (FileInputStream fileInputStream = new FileInputStream(plaintextFile);
             FileOutputStream fileOutputStream = new FileOutputStream(encryptedFile);
             CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher)
        ) {
            byte[] inputStreamByte = fileInputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                cipherOutputStream.write(inputStreamByte);
                inputStreamByte = fileInputStream.readNBytes(1024);
            }
        }
        Assertions.assertTrue(encryptedFile.exists());
        Assertions.assertFalse(cryptoUtility.decryptFile(packageEntity, encryptedFile, pathForDecryptedFile));
        packageEntity.setInitializationVector(cipher.getIV());
        Assertions.assertTrue(cryptoUtility.decryptFile(packageEntity, encryptedFile, pathForDecryptedFile));
        Assertions.assertArrayEquals(Files.readAllBytes(plaintextFile.toPath()), Files.readAllBytes(pathForDecryptedFile));
    }

    @Test
    void calculateChecksum_invalid() {
        Assertions.assertNull(cryptoUtility.calculateChecksum(null));
    }

    @Test
    void calculateChecksum_valid() throws IOException, NoSuchAlgorithmException {
        new FileOutputStream(plaintextFile).write("Test Content".getBytes(StandardCharsets.UTF_8));
        FileInputStream fileInputStream = new FileInputStream(plaintextFile);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA3-512");
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            messageDigest.update(buffer, 0, bytesRead);
        }
        byte[] checkSum = messageDigest.digest();
        StringBuilder stringBuilder = new StringBuilder(checkSum.length * 2);
        for (byte b : checkSum) {
            stringBuilder.append(String.format("%02x", b));
        }
        Assertions.assertEquals(stringBuilder.toString(), cryptoUtility.calculateChecksum(new FileInputStream(plaintextFile)));
    }
}
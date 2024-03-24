package org.codesystem.server.utility;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
import org.codesystem.server.entity.AgentEntity;
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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
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
    }

    @AfterEach
    void tearDown() {
        serverRepository.deleteAll();
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
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC(null, null));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC("Test Message".getBytes(StandardCharsets.UTF_8), null));
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.encryptECC("Test Message".getBytes(StandardCharsets.UTF_8), new AgentEntity()));
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
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
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.createSignatureECC(null));
    }

    @Test
    void verifySignatureECC_valid() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        byte[] messageSignature = cryptoUtility.createSignatureECC("Test Message");
        Signature signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
        signature.initVerify(serverPublicKey);
        signature.update("Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(signature.verify(messageSignature));
        signature.update("Wrong Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertFalse(signature.verify(messageSignature));
    }
}
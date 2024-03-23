package org.codesystem.server.utility;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.server.configuration.SecurityConfiguration;
import org.codesystem.server.configuration.ServerInitialization;
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

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CryptoUtilityTest {
    private static MariaDB4jSpringService DB;

    @Autowired
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

    @BeforeAll
    public static void init() {
        DB = new MariaDB4jSpringService();
        DB.setDefaultPort(3307);
        DB.setDefaultOsUser("root");
        DB.start();
    }

    @AfterAll
    public static void cleanupDB() {
        if (DB != null) DB.stop();
    }

    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        MockitoAnnotations.initMocks(this);

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
    }

    @AfterEach
    void tearDown() {
        serverRepository.deleteAll();
    }

    @Test
    void decryptECC() {
        Assertions.assertThrows(RuntimeException.class, () -> cryptoUtility.decryptECC("".getBytes(StandardCharsets.UTF_8)));
    }
}
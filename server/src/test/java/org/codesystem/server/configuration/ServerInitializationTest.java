package org.codesystem.server.configuration;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.exception.TestSystemExitException;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.utility.SystemExitUtility;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ServerInitializationTest {
    private static DB db;
    PrivateKey serverPrivateKey;
    PublicKey serverPublicKey;
    private ServerInitialization serverInitialization;
    private ServerRepository serverRepository;
    private ResourceLoader resourceLoader;

    @BeforeAll
    public static void init() throws ManagedProcessException {
        DBConfiguration dbConfiguration = DBConfigurationBuilder.newBuilder()
                .setPort(3307)
                .build();
        db = DB.newEmbeddedDB(dbConfiguration);
        db.start();
    }

    @AfterAll
    public static void cleanupDB() throws ManagedProcessException {
        if (db != null) db.stop();
    }

    @BeforeEach
    public void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        serverPrivateKey = keyPair.getPrivate();
        serverPublicKey = keyPair.getPublic();

        serverRepository = Mockito.spy(ServerRepository.class);
        SystemExitUtility systemExitUtility = Mockito.mock(SystemExitUtility.class);
        resourceLoader = Mockito.mock(ResourceLoader.class);
        Mockito.doThrow(TestSystemExitException.class).when(systemExitUtility).exit(Mockito.anyInt());
        serverInitialization = new ServerInitialization(serverRepository, resourceLoader, systemExitUtility);
    }

    @Test
    void initializeServer_emptyDatabase() {
        Assertions.assertThrows(TestSystemExitException.class, () -> serverInitialization.initializeServer());
        Mockito.verify(serverRepository).save(Mockito.any());
    }

    @Test
    void initializeServer_tooManyServerEntries() {
        Mockito.when(serverRepository.findAll()).thenReturn(List.of(new ServerEntity(), new ServerEntity()));
        Assertions.assertThrows(TestSystemExitException.class, () -> serverInitialization.initializeServer());
    }

    @Test
    void initializeServer_serverExistingInvalidPublicKey() {
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString("Public Key".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(serverRepository.findAll()).thenReturn(List.of(serverEntity));
        Assertions.assertThrows(RuntimeException.class, () -> serverInitialization.initializeServer());
    }

    @Test
    void initializeServer_serverExistingInvalidPrivateKey() {
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        serverEntity.setPrivateKeyBase64(Base64.getEncoder().encodeToString("Private Key".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(serverRepository.findAll()).thenReturn(List.of(serverEntity));
        Assertions.assertThrows(RuntimeException.class, () -> serverInitialization.initializeServer());
    }

    @Test
    void initializeServer_serverExistingMissingResource() {
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        serverEntity.setPrivateKeyBase64(Base64.getEncoder().encodeToString(serverPrivateKey.getEncoded()));
        Mockito.when(serverRepository.findAll()).thenReturn(List.of(serverEntity));
        Assertions.assertThrows(RuntimeException.class, () -> serverInitialization.initializeServer());
    }

    @Test
    void initializeServer_serverExisting() {
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        serverEntity.setPrivateKeyBase64(Base64.getEncoder().encodeToString(serverPrivateKey.getEncoded()));
        Mockito.when(serverRepository.findAll()).thenReturn(List.of(serverEntity));
        Mockito.when(resourceLoader.getResource(Mockito.any())).thenReturn(new ClassPathResource("Test-File"));
        Assertions.assertDoesNotThrow(() -> serverInitialization.initializeServer());
    }
}
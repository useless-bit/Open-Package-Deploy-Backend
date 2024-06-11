package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.utility.SystemExitUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.Reader;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

class PropertiesLoaderTest {
    PropertiesLoader propertiesLoader;
    File propertiesFile = Paths.get("opd-agent.properties").toFile();
    KeyFactory keyFactory;
    PrivateKey agentPrivateKey;
    PublicKey agentPublicKey;
    PrivateKey serverPrivateKey;
    PublicKey serverPublicKey;
    MockedStatic<SystemExitUtility> systemExitMockedStatic;

    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        agentPrivateKey = keyPair.getPrivate();
        agentPublicKey = keyPair.getPublic();
        keyPair = keyPairGenerator.generateKeyPair();
        serverPrivateKey = keyPair.getPrivate();
        serverPublicKey = keyPair.getPublic();


        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);
        propertiesLoader = null;
        propertiesFile.delete();
    }

    @AfterEach
    void tearDown() {
        propertiesFile.delete();
        systemExitMockedStatic.close();
    }

    @Test
    void saveProperties_noFilePresent() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.saveProperties();
        Assertions.assertTrue(propertiesFile.exists());
    }

    @Test
    void loadProperties_noFilePresent() {
        propertiesLoader = new PropertiesLoader();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.loadProperties());
    }

    @Test
    void load_reader() {
        propertiesLoader = new PropertiesLoader();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.load(Reader.nullReader()));
    }

    @Test
    void loadProperties_invalidUrl() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "htttps://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "false");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.loadProperties());
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "invalid URL");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "false");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_invalidServerRegistered() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "https://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "invalid value");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_invalidKeys() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "https://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "true");
        propertiesLoader.put("Server.ECC.Public-Key", "Invalid Key");
        propertiesLoader.put("Agent.ECC.Public-Key", "Invalid Key");
        propertiesLoader.put("Agent.ECC.Private-Key", "Invalid Key");
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_invalidUpdateInterval() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "https://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "false");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "invalid Value");
        propertiesLoader.saveProperties();
        Assertions.assertThrows(TestSystemExitException.class, () -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_validNotRegisteredNoKeys() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "https://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "false");
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertDoesNotThrow(() -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_validNotRegisteredWithKeys() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "https://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "false");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertDoesNotThrow(() -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_validRegisteredWithKeys() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "https://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "true");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertDoesNotThrow(() -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_validRegisteredWithKeysHttp() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "http://localhost");
        propertiesLoader.put("Server.Registration-Token", "Registration Token");
        propertiesLoader.put("Server.Registered", "true");
        propertiesLoader.put("Server.ECC.Public-Key", Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Public-Key", Base64.getEncoder().encodeToString(agentPublicKey.getEncoded()));
        propertiesLoader.put("Agent.ECC.Private-Key", Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        propertiesLoader.put("Agent.Update-Interval", "30");
        propertiesLoader.saveProperties();
        Assertions.assertDoesNotThrow(() -> propertiesLoader.loadProperties());
    }

    @Test
    void loadProperties_validEmpty() {
        propertiesLoader = new PropertiesLoader();
        propertiesLoader.put("Server.Url", "http://localhost");
        propertiesLoader.saveProperties();
        Assertions.assertDoesNotThrow(() -> propertiesLoader.loadProperties());
    }
}
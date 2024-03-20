package org.codesystem.utility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.PropertiesLoader;
import org.codesystem.TestSystemExitException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

class CryptoUtilityTest {
    MockedStatic<SystemExitUtility> systemExitMockedStatic;
    PropertiesLoader propertiesLoader;
    CryptoUtility cryptoUtility;
    KeyFactory keyFactory;
    PrivateKey agentPrivateKey;
    PublicKey agentPublicKey;
    PrivateKey serverPrivateKey;
    PublicKey serverPublicKey;
    Cipher cipherEcc;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        Security.addProvider(new BouncyCastleProvider());
        keyFactory = KeyFactory.getInstance("EC");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        agentPrivateKey = keyPair.getPrivate();
        agentPublicKey = keyPair.getPublic();
        keyPair = keyPairGenerator.generateKeyPair();
        serverPrivateKey = keyPair.getPrivate();
        serverPublicKey = keyPair.getPublic();

        this.cipherEcc = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cryptoUtility = null;
        propertiesLoader = Mockito.mock(PropertiesLoader.class);

        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);
    }

    @AfterEach
    void tearDown() {
        systemExitMockedStatic.close();
    }

    @Test
    void constructor_invalid() {
        Assertions.assertThrows(TestSystemExitException.class, () -> new CryptoUtility(propertiesLoader));
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn("");
        Assertions.assertThrows(TestSystemExitException.class, () -> new CryptoUtility(propertiesLoader));
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn("invalid Key");
        Assertions.assertThrows(TestSystemExitException.class, () -> new CryptoUtility(propertiesLoader));
    }

    @Test
    void constructor_valid() {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Assertions.assertThrows(TestSystemExitException.class, () -> new CryptoUtility(propertiesLoader));

    }
}
package org.codesystem.utility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.codesystem.PropertiesLoader;
import org.codesystem.TestSystemExitException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

class CryptoUtilityTest {
    private final IESParameterSpec iesParamSpec = new IESParameterSpec(
            /* derivation = */ null,
            /* encoding = */ null,
            /* macKeySize = */ 1024,
            /* cipherKeySize = */ 1024,
            /* nonce = */ null,
            /* usePointCompression = */ false);
    MockedStatic<SystemExitUtility> systemExitMockedStatic;
    PropertiesLoader propertiesLoader;
    CryptoUtility cryptoUtility;
    KeyFactory keyFactory;
    PrivateKey agentPrivateKey;
    PublicKey agentPublicKey;
    PrivateKey serverPrivateKey;
    PublicKey serverPublicKey;
    Cipher cipherEcc;
    Signature signature;

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
        this.signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);

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
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Assertions.assertThrows(TestSystemExitException.class, () -> new CryptoUtility(propertiesLoader));
    }

    @Test
    void constructor_valid() {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn("");
        Assertions.assertDoesNotThrow(() -> new CryptoUtility(propertiesLoader));
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn("   ");
        Assertions.assertDoesNotThrow(() -> new CryptoUtility(propertiesLoader));
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        Assertions.assertDoesNotThrow(() -> new CryptoUtility(propertiesLoader));
    }

    @Test
    void decryptECC_invalid() {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.decryptECC(null));
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.decryptECC("Test Message".getBytes(StandardCharsets.UTF_8)));

    }

    @Test
    void decryptECC_valid() throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        cipherEcc.init(Cipher.ENCRYPT_MODE, agentPublicKey, iesParamSpec);
        byte[] encryptedMessage = cipherEcc.doFinal("Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals("Test Message", cryptoUtility.decryptECC(encryptedMessage));
    }

    @Test
    void encryptECC_invalid() {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.encryptECC(null));
    }

    @Test
    void encryptECC_valid() throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        cipherEcc.init(Cipher.DECRYPT_MODE, serverPrivateKey, iesParamSpec);
        byte[] encryptedMessage = cryptoUtility.encryptECC("Test Message".getBytes(StandardCharsets.UTF_8));
        String decryptedMessage = new String(cipherEcc.doFinal(encryptedMessage), StandardCharsets.UTF_8);
        Assertions.assertEquals("Test Message", decryptedMessage);
    }

    @Test
    void createSignature_invalid() {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.createSignatureECC(null));
    }

    @Test
    void createSignature_valid() throws InvalidKeyException, SignatureException {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        byte[] signatureMessage = cryptoUtility.createSignatureECC("Test Message");
        signature.initVerify(agentPublicKey);
        signature.update("Test Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(signature.verify(signatureMessage));
    }

    @Test
    void verifySignature_invalid() {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.verifySignatureECC(null, null));
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.verifySignatureECC("Message", null));
        Assertions.assertThrows(TestSystemExitException.class, () -> cryptoUtility.verifySignatureECC(null, "Signature".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void verifySignature_valid() throws InvalidKeyException, SignatureException {
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Private-Key")).thenReturn(Base64.getEncoder().encodeToString(agentPrivateKey.getEncoded()));
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));
        cryptoUtility = new CryptoUtility(propertiesLoader);
        signature.initSign(serverPrivateKey);
        signature.update("Test Message".getBytes(StandardCharsets.UTF_8));
        byte[] generatedSignature = signature.sign();
        Assertions.assertTrue(cryptoUtility.verifySignatureECC("Test Message", generatedSignature));
        Assertions.assertFalse(cryptoUtility.verifySignatureECC("Invalid Message", generatedSignature));
        signature.update("Other Message".getBytes(StandardCharsets.UTF_8));
        Assertions.assertFalse(cryptoUtility.verifySignatureECC("Test Message", signature.sign()));
    }
}
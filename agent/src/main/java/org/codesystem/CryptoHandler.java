package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoHandler {

    private final KeyFactory keyFactory;
    private final Cipher cipher;
    private final Cipher cipherAES;

    private final Signature signature;
    private final PrivateKey privateKeyAgent;
    private final PublicKey publicKeyServer;
    private final IESParameterSpec iesParamSpec = new IESParameterSpec(
            /* derivation = */ null,
            /* encoding = */ null,
            /* macKeySize = */ 1024,
            /* cipherKeySize = */ 1024,
            /* nonce = */ null,
            /* usePointCompression = */ false);

    public CryptoHandler() {
        try {
            this.keyFactory = KeyFactory.getInstance("EC");
            this.cipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
            this.cipherAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);

            this.privateKeyAgent = this.keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(AgentApplication.properties.getProperty("Agent.ECC.Private-Key"))));
            //load with null when Server Key was not retrieved yet
            if (!AgentApplication.properties.getProperty("Server.ECC.Public-Key").isBlank()) {
                this.publicKeyServer = this.keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(AgentApplication.properties.getProperty("Server.ECC.Public-Key"))));
            } else {
                this.publicKeyServer = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decryptECC(byte[] message) {
        byte[] decryptedMessage;
        try {
            cipher.init(Cipher.DECRYPT_MODE, this.privateKeyAgent, iesParamSpec);
            decryptedMessage = cipher.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt the message: " + e.getMessage());
        }
        return new String(decryptedMessage, StandardCharsets.UTF_8);
    }

    public byte[] encryptECC(byte[] message) {
        byte[] encryptedMessage;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKeyServer, iesParamSpec);
            encryptedMessage = cipher.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load the Public-Key: " + e.getMessage());
        }
        return encryptedMessage;
    }

    public byte[] createSignatureECC(String message) {
        byte[] signatureForMessage;
        try {
            signature.initSign(privateKeyAgent);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            signatureForMessage = signature.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return signatureForMessage;
    }

    public boolean verifySignatureECC(String message, String base64Signature) {
        try {
            signature.initVerify(publicKeyServer);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean decryptFile(SecretKey secretKey, byte[] initializationVector, File encryptedFile, Path targetPath) {
        Cipher cipher = cipherAES;
        try (
                FileInputStream fileInputStream = new FileInputStream(encryptedFile);
                CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
                FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toString())
        ) {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(initializationVector));
            byte[] inputStreamByte = cipherInputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                fileOutputStream.write(inputStreamByte);
                inputStreamByte = cipherInputStream.readNBytes(1024);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String calculateChecksumOfFile(String filePath) {
        try(FileInputStream fileInputStream = new FileInputStream(filePath)) {

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA3-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        byte[] buffer = new byte[4096];
        int bytesRead;

        while (true) {
            try {
                if ((bytesRead = fileInputStream.read(buffer)) == -1) break;
            } catch (IOException e) {
                return null;
            }
            messageDigest.update(buffer, 0, bytesRead);
        }

        byte[] checkSum = messageDigest.digest();

        StringBuilder stringBuilder = new StringBuilder(checkSum.length * 2);
        for (byte b : checkSum) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

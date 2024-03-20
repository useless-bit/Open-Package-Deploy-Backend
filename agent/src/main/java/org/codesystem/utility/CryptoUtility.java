package org.codesystem.utility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.codesystem.PropertiesLoader;
import org.codesystem.exceptions.SevereAgentErrorException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtility {
    private final Cipher cipherEcc;
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

    public CryptoUtility(PropertiesLoader propertiesLoader) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            this.cipherEcc = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            this.cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
            this.signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);

            this.privateKeyAgent = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(propertiesLoader.getProperty("Agent.ECC.Private-Key"))));
            //load with null when Server Key was not retrieved yet
            if (!propertiesLoader.getProperty("Server.ECC.Public-Key").isBlank()) {
                this.publicKeyServer = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(propertiesLoader.getProperty("Server.ECC.Public-Key"))));
            } else {
                this.publicKeyServer = null;
            }
        } catch (Exception e) {
            throw new SevereAgentErrorException("Cannot initialize Crypto Utility: " + e.getMessage());
        }
    }

    public String decryptECC(byte[] message) {
        byte[] decryptedMessage = null;
        try {
            cipherEcc.init(Cipher.DECRYPT_MODE, this.privateKeyAgent, iesParamSpec);
            decryptedMessage = cipherEcc.doFinal(message);
        } catch (Exception e) {
            throw new SevereAgentErrorException("Unable to decrypt the message: " + e.getMessage());
        }
        return new String(decryptedMessage, StandardCharsets.UTF_8);
    }

    public byte[] encryptECC(byte[] message) {
        byte[] encryptedMessage;
        try {
            cipherEcc.init(Cipher.ENCRYPT_MODE, publicKeyServer, iesParamSpec);
            encryptedMessage = cipherEcc.doFinal(message);
        } catch (Exception e) {
            throw new SevereAgentErrorException("Unable to load the Public-Key: " + e.getMessage());
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
            throw new SevereAgentErrorException("Unable to create ECC-Signature: " + e.getMessage());
        }
        return signatureForMessage;
    }

    public boolean verifySignatureECC(String message, String base64Signature) {
        try {
            signature.initVerify(publicKeyServer);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception e) {
            throw new SevereAgentErrorException("Unable to load the Public-Key: " + e.getMessage());
        }
    }

    public boolean decryptFile(SecretKey secretKey, byte[] initializationVector, File encryptedFile, Path targetPath) {
        Cipher cipher = cipherAES;
        try (
                FileInputStream fileInputStream = new FileInputStream(encryptedFile);
                CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
                FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toString())
        ) {
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
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
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {

            MessageDigest messageDigest;
            messageDigest = MessageDigest.getInstance("SHA3-512");

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
            return stringBuilder.toString();
        } catch (Exception e) {
            throw new SevereAgentErrorException("Unable to calculate checksum: " + e.getMessage());
        }
    }
}

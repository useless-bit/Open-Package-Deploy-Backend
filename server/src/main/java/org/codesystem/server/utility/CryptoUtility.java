package org.codesystem.server.utility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.exception.CryptoUtilityException;
import org.codesystem.server.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Service
public class CryptoUtility {
    private final KeyFactory keyFactory;
    private final KeyGenerator keyGeneratorAES;
    private final PrivateKey privateKeyServer;
    private final IESParameterSpec iesParamSpec = new IESParameterSpec(
            /* derivation = */ null,
            /* encoding = */ null,
            /* macKeySize = */ 1024,
            /* cipherKeySize = */ 1024,
            /* nonce = */ null,
            /* usePointCompression = */ false);

    @Autowired
    public CryptoUtility(ServerRepository serverRepository) {
        List<ServerEntity> serverEntityList = serverRepository.findAll();

        try {
            this.keyFactory = KeyFactory.getInstance("EC");
            this.keyGeneratorAES = KeyGenerator.getInstance("AES");
            this.keyGeneratorAES.init(256, new SecureRandom());
            if (!serverEntityList.isEmpty()) {
                this.privateKeyServer = this.keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(serverEntityList.get(0).getPrivateKeyBase64())));
            } else {
                this.privateKeyServer = null;
            }
        } catch (Exception e) {
            throw new CryptoUtilityException("Cannot create the class: " + e.getMessage());
        }
    }

    public String decryptECC(byte[] message) {
        byte[] decryptedMessage;
        try {
            Cipher cipher = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, this.privateKeyServer, iesParamSpec);
            decryptedMessage = cipher.doFinal(message);
        } catch (Exception e) {
            throw new CryptoUtilityException("Unable to decrypt the message: " + e.getMessage());
        }
        return new String(decryptedMessage, StandardCharsets.UTF_8);
    }

    public byte[] encryptECC(byte[] message, AgentEntity agentEntity) {
        byte[] encryptedMessage;
        try {
            Cipher cipher = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, loadPublicKeyFromAgentEntity(agentEntity), iesParamSpec);
            encryptedMessage = cipher.doFinal(message);
        } catch (Exception e) {
            throw new CryptoUtilityException("Unable to load the Public-Key: " + e.getMessage());
        }
        return encryptedMessage;
    }

    public byte[] createSignatureECC(String message) {
        byte[] signatureForMessage;
        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
            signature.initSign(privateKeyServer);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            signatureForMessage = signature.sign();
        } catch (Exception e) {
            throw new CryptoUtilityException("Unable to create the signature: " + e.getMessage());
        }
        return signatureForMessage;
    }

    public boolean verifySignatureECC(String message, String base64Signature, AgentEntity agentEntity) {
        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
            signature.initVerify(loadPublicKeyFromAgentEntity(agentEntity));
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception e) {
            throw new CryptoUtilityException("Unable to verify the signature: " + e.getMessage());
        }
    }

    public boolean encryptFile(PackageEntity packageEntity, File plaintextFile, Path targetFilePath) {
        packageEntity.setEncryptionToken(keyGeneratorAES.generateKey());
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, packageEntity.getEncryptionToken());
        } catch (Exception e) {
            return false;
        }
        packageEntity.setInitializationVector(cipher.getIV());
        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFilePath.toString());
             FileInputStream fileInputStream = new FileInputStream(plaintextFile);
             CipherOutputStream cipherOut = new CipherOutputStream(fileOutputStream, cipher)) {

            byte[] inputStreamByte = fileInputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                cipherOut.write(inputStreamByte);
                inputStreamByte = fileInputStream.readNBytes(1024);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean decryptFile(PackageEntity packageEntity, File encryptedFile, Path targetPath) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        } catch (Exception e) {
            return false;
        }
        try (
                FileInputStream fileInputStream = new FileInputStream(encryptedFile);
                CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
                FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toString())
        ) {
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, packageEntity.getInitializationVector());
            cipher.init(Cipher.DECRYPT_MODE, packageEntity.getEncryptionToken(), gcmParameterSpec);
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


    public String calculateChecksum(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA3-512");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        byte[] buffer = new byte[4096];
        int bytesRead;

        while (true) {
            try {
                if ((bytesRead = inputStream.read(buffer)) == -1) break;
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
    }

    private PublicKey loadPublicKeyFromAgentEntity(AgentEntity agentEntity) {
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(agentEntity.getPublicKeyBase64())));
        } catch (Exception e) {
            throw new CryptoUtilityException("Unable to load the Agent-PublicKey: " + e.getMessage());
        }
    }
}

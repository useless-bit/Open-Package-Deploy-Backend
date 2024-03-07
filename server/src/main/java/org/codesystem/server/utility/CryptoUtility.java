package org.codesystem.server.utility;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@DependsOn("serverInitialization")
public class CryptoUtility {

    private final ServerRepository serverRepository;
    private final PackageRepository packageRepository;

    private final KeyFactory keyFactory;
    private final KeyGenerator keyGeneratorAES;
    private final Cipher cipherECC;
    private final Cipher cipherAES;
    private final Signature signature;
    private final PrivateKey privateKeyServer;
    private final IESParameterSpec iesParamSpec = new IESParameterSpec(
            /* derivation = */ null,
            /* encoding = */ null,
            /* macKeySize = */ 1024,
            /* cipherKeySize = */ 1024,
            /* nonce = */ null,
            /* usePointCompression = */ false);

    @Autowired
    public CryptoUtility(ServerRepository serverRepository, PackageRepository packageRepository) {
        this.serverRepository = serverRepository;
        this.packageRepository = packageRepository;
        ServerEntity serverEntity = this.serverRepository.findAll().get(0);
        try {
            this.keyFactory = KeyFactory.getInstance("EC");
            this.keyGeneratorAES = KeyGenerator.getInstance("AES");
            this.keyGeneratorAES.init(128);
            this.cipherECC = Cipher.getInstance("ECIES/None/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            this.cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
            this.signature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
            this.privateKeyServer = this.keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(serverEntity.getPrivateKeyBase64())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decryptECC(byte[] message) {
        byte[] decryptedMessage;
        Cipher cipher = cipherECC;
        try {
            cipher.init(Cipher.DECRYPT_MODE, this.privateKeyServer, iesParamSpec);
            decryptedMessage = cipher.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt the message: " + e.getMessage());
        }
        return new String(decryptedMessage, StandardCharsets.UTF_8);
    }

    public byte[] encryptECC(byte[] message, AgentEntity agentEntity) {
        byte[] encryptedMessage;
        Cipher cipher = cipherECC;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, loadPublicKeyFromAgentEntity(agentEntity), iesParamSpec);
            encryptedMessage = cipher.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load the Public-Key: " + e.getMessage());
        }
        return encryptedMessage;
    }

    public byte[] createSignatureECC(String message) {
        byte[] signatureForMessage;
        try {
            signature.initSign(privateKeyServer);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            signatureForMessage = signature.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return signatureForMessage;
    }

    public boolean verifySignatureECC(String message, String base64Signature, AgentEntity agentEntity) {
        try {
            signature.initVerify(loadPublicKeyFromAgentEntity(agentEntity));
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean encryptFile(PackageEntity packageEntity, File plaintextFile, Path targetFilePath) {
        packageEntity.setEncryptionToken(keyGeneratorAES.generateKey());
        Cipher cipher = cipherAES;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, packageEntity.getEncryptionToken());
        } catch (InvalidKeyException e) {
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
        Cipher cipher = cipherAES;
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
            System.out.println(e);
            return false;
        }
        return true;
    }


    public String calculateChecksum(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        MessageDigest messageDigest = null;
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
            throw new RuntimeException(e);
        }
    }
}

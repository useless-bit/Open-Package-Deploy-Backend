package org.codesystem.server.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.utility.CryptoUtility;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServerInitialization {
    private final ServerRepository serverRepository;
    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void initializeServer() {
        if (serverRepository.findAll().size() > 1) {
            //should not be possible
            System.exit(1);
        } else if (serverRepository.findAll().isEmpty()) {
            serverRepository.save(generateServerEntity());
        } else {
            validateServerEntity();
        }
        calculateAgentChecksum();
    }

    private String calculateChecksum(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            return DigestUtils.md5DigestAsHex(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    private void calculateAgentChecksum() {
        Resource resource = resourceLoader.getResource("classpath:agent/Agent.jar");
        String checksum;
        try {
            checksum = calculateChecksum(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        serverEntity.setAgentChecksum(checksum);
        serverRepository.save(serverEntity);
    }

    private void validateServerEntity() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);

        KeyFactory keyFactory;
        try {
            //load KeyFactory and public Key
            keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec x509EncodedKeySpecPublicKey = new X509EncodedKeySpec(Base64.getDecoder().decode(serverEntity.getPublicKeyBase64()));
            keyFactory.generatePublic(x509EncodedKeySpecPublicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to load the KeyFactory Algorithm: " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Unable to load the Public-Key: " + e.getMessage());
        }
        try {
            //load private Key
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(serverEntity.getPrivateKeyBase64()));
            keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Unable to load the Private-Key: " + e.getMessage());
        }
    }

    private ServerEntity generateServerEntity() {
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to load the KeyFactory Algorithm: " + e.getMessage());
        } catch (NoSuchProviderException e) {
            throw new RuntimeException("Unable to load the Algorithm Provider: " + e.getMessage());
        }
        try {
            keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Unable to load the Algorithm: " + e.getMessage());
        }
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentRegistrationToken(UUID.randomUUID().toString());
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        serverEntity.setPrivateKeyBase64(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        return serverEntity;
    }
}

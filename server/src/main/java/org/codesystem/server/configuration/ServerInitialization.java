package org.codesystem.server.configuration;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.utility.CryptoUtility;
import org.codesystem.server.utility.SystemExitUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
public class ServerInitialization {
    private final ServerRepository serverRepository;
    private final ResourceLoader resourceLoader;
    private final CryptoUtility cryptoUtility;
    private final SystemExitUtility systemExitUtility;
    private final Logger logger = LoggerFactory.getLogger(ServerInitialization.class);

    public ServerInitialization(ServerRepository serverRepository, ResourceLoader resourceLoader, SystemExitUtility systemExitUtility) {
        this.serverRepository = serverRepository;
        this.resourceLoader = resourceLoader;
        this.cryptoUtility = new CryptoUtility(serverRepository);
        this.systemExitUtility = systemExitUtility;
    }

    @PostConstruct
    public void initializeServer() {
        if (serverRepository.findAll().size() > 1) {
            // should not be possible
            systemExitUtility.exit(1);
        } else if (serverRepository.findAll().isEmpty()) {
            serverRepository.save(generateServerEntity());
            systemExitUtility.exit(0);
        } else {
            validateServerEntity();
        }
        calculateAgentChecksum();
    }

    private void calculateAgentChecksum() {
        Resource resource = resourceLoader.getResource("classpath:agent/Agent.jar");
        String checksum;
        try {
            checksum = cryptoUtility.calculateChecksum(resource.getInputStream());
        } catch (Exception e) {
            logger.error("Error encountered when calculating Agent-checksum: {}", e.getMessage());
            systemExitUtility.exit(-100);
            return;
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
        } catch (Exception e) {
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
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load KeyPair Generator: " + e.getMessage());
        }
        try {
            keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Unable to load the KeyPair Generator Parameter: " + e.getMessage());
        }
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setAgentRegistrationToken(UUID.randomUUID() + "-" + (UUID.randomUUID()));
        serverEntity.setPublicKeyBase64(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        serverEntity.setPrivateKeyBase64(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        serverEntity.setAgentChecksum("");
        return serverEntity;
    }
}

package org.codesystem.server;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@SpringBootTest
public class ServerApplicationTest {
    public static final String PACKAGE_LOCATION = "/opt/OPD/Packages/";

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(ServerApplicationTest.class, args);
    }

}

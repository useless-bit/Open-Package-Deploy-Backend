package org.codesystem.server;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.Security;

@SpringBootTest
public class ServerApplicationTest {

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(ServerApplicationTest.class, args);
    }

}

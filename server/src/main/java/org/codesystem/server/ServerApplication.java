package org.codesystem.server;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.security.Security;

@EnableScheduling
@SpringBootApplication
public class ServerApplication {
    public static final String PACKAGE_LOCATION = File.separator + "opt" + File.separator + "OPD" + File.separator + "Packages" + File.separator;

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(ServerApplication.class, args);
    }

}

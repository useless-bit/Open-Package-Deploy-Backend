package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.utility.CryptoUtility;

import java.security.Security;

public class Main {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        PropertiesLoader propertiesLoader = new PropertiesLoader();
        propertiesLoader.loadProperties();
        CryptoUtility cryptoUtility = new CryptoUtility(propertiesLoader);

        new AgentApplication(
                propertiesLoader,
                new HardwareInfo(),
                cryptoUtility
        ).agentLogic();
    }
}

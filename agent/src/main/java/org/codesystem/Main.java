package org.codesystem;

import org.codesystem.utility.CryptoUtility;

public class Main {
    public static void main(String[] args) {
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        CryptoUtility cryptoUtility = new CryptoUtility(propertiesLoader);


        new AgentApplication(
                propertiesLoader,
                new HardwareInfo(),
                cryptoUtility
        ).agentLogic();
    }
}

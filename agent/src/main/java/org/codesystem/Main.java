package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.utility.AutoUpdateUtility;
import org.codesystem.utility.CryptoUtility;

import java.io.File;
import java.net.URISyntaxException;
import java.security.Security;

public class Main {
    public static void main(String[] args) {
        String filename;
        try {
            filename = String.valueOf(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException e) {
            throw new SevereAgentErrorException("Cannot get filename");
        }
        if (filename.endsWith(Variables.FILE_NAME_AGENT_UPDATE)) {
            new AutoUpdateUtility().updateApplication();
        }

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

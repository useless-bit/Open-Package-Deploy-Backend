package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.utility.AutoUpdateUtility;
import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.DownloadUtility;
import org.codesystem.utility.PackageUtility;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AgentApplication {
    public static final Logger logger = Logger.getLogger("");
    private static final PropertiesLoader propertiesLoader = new PropertiesLoader();
    private static String agentChecksum;
    private static OperatingSystem operatingSystem;

    public static void main(String[] args) {
        initialSetup();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                AgentApplication.mainLogic();
            } catch (Throwable t) {
                throw new SevereAgentErrorException("Unexpected Exception occurred: " + t.getMessage());
            }
        }, 0, Integer.parseInt(propertiesLoader.getProperty(Variables.PROPERTIES_AGENT_UPDATE_INTERVAL)), TimeUnit.SECONDS);
    }

    private static void initialSetup() {
        String filename;
        try {
            filename = String.valueOf(new File(AgentApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException e) {
            throw new SevereAgentErrorException("Cannot get filename");
        }
        if (filename.endsWith(Variables.FILE_NAME_AGENT_UPDATE)) {
            logger.info("Entering Update Mode");
            new AutoUpdateUtility().updateApplication();
        } else if (Files.exists(Paths.get(Variables.FILE_NAME_AGENT_UPDATE))) {
            try {
                Files.deleteIfExists(Paths.get(Variables.FILE_NAME_AGENT_UPDATE));
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old update");
            }
        }
        HardwareInfo hardwareInfo = new HardwareInfo();
        if (!hardwareInfo.isElevated()) {
            throw new SevereAgentErrorException("Application needs elevated permissions");
        }

        if (hardwareInfo.getOsManufacturer().toLowerCase().contains("linux")) {
            AgentApplication.operatingSystem = OperatingSystem.LINUX;
        } else {
            throw new SevereAgentErrorException("Unsupported OS");
        }
        Security.addProvider(new BouncyCastleProvider());
        logger.info("Agent Startup");
        propertiesLoader.loadProperties();

        CryptoUtility cryptoUtility = new CryptoUtility(propertiesLoader);
        agentChecksum = cryptoUtility.calculateChecksumOfFile("Agent.jar");

        UpdateHandler updateHandler = new UpdateHandler(new DownloadUtility(), cryptoUtility, propertiesLoader);
        PackageUtility packageUtility = new PackageUtility(operatingSystem, propertiesLoader);
        ServerCommunicationRegistration serverCommunicationRegistration = new ServerCommunicationRegistration(cryptoUtility, propertiesLoader, agentChecksum, updateHandler, packageUtility);
        serverCommunicationRegistration.validateRegistration();
    }

    private static void mainLogic() {
        logger.info("Checking for deployment");
        ServerCommunication serverCommunication = new ServerCommunication(new CryptoUtility(propertiesLoader), propertiesLoader, agentChecksum, new UpdateHandler(new DownloadUtility(), new CryptoUtility(propertiesLoader), propertiesLoader), new PackageUtility(operatingSystem, propertiesLoader));
        serverCommunication.waitForServerAvailability();
        while (serverCommunication.sendUpdateRequest()) {
            logger.info("Checking for deployment");
        }
    }
}
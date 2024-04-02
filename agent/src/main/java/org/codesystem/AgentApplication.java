package org.codesystem;

import org.codesystem.enums.OperatingSystem;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.DetailedSystemInformation;
import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.DownloadUtility;
import org.codesystem.utility.PackageUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AgentApplication {
    public static final Logger logger = Logger.getLogger("Agent");
    private final PropertiesLoader propertiesLoader;
    private final HardwareInfo hardwareInfo;
    private final CryptoUtility cryptoUtility;
    private String agentChecksum;
    private OperatingSystem operatingSystem;

    public AgentApplication(PropertiesLoader propertiesLoader, HardwareInfo hardwareInfo, CryptoUtility cryptoUtility) {
        this.propertiesLoader = propertiesLoader;
        this.hardwareInfo = hardwareInfo;
        this.cryptoUtility = cryptoUtility;
    }

    public void agentLogic() {
        initialSetup();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                mainLogic();
            } catch (Throwable t) {
                throw new SevereAgentErrorException("Unexpected Exception occurred: " + t.getMessage());
            }
        }, 0, Integer.parseInt(propertiesLoader.getProperty(Variables.PROPERTIES_AGENT_UPDATE_INTERVAL)), TimeUnit.SECONDS);
    }

    private void initialSetup() {
        if (Files.exists(Variables.PATH_UPDATE_FILE)) {
            try {
                Files.deleteIfExists(Variables.PATH_UPDATE_FILE);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old update");
            }
        }
        if (!hardwareInfo.isElevated()) {
            throw new SevereAgentErrorException("Application needs elevated permissions");
        }

        DetailedSystemInformation detailedSystemInformation = new DetailedSystemInformation(hardwareInfo);
        if (detailedSystemInformation.getOperatingSystem() == null) {
            throw new SevereAgentErrorException("Unsupported OS: " + hardwareInfo.getOsManufacturer());
        } else {
            operatingSystem = detailedSystemInformation.getOperatingSystem();
            logger.info("Detected OS: " + detailedSystemInformation.getOperatingSystem());
        }
        logger.info("Agent Startup");

        agentChecksum = cryptoUtility.calculateChecksumOfFile("Agent.jar");

        UpdateHandler updateHandler = new UpdateHandler(new DownloadUtility(), cryptoUtility, propertiesLoader);
        PackageUtility packageUtility = new PackageUtility(cryptoUtility, operatingSystem, propertiesLoader, new DownloadUtility());
        ServerCommunicationRegistration serverCommunicationRegistration = new ServerCommunicationRegistration(cryptoUtility, propertiesLoader, new ServerCommunication(cryptoUtility, propertiesLoader, agentChecksum, updateHandler, packageUtility));
        serverCommunicationRegistration.validateRegistration();
    }

    private void mainLogic() {
        logger.info("Checking for deployment");
        ServerCommunication serverCommunication = new ServerCommunication(cryptoUtility, propertiesLoader, agentChecksum, new UpdateHandler(new DownloadUtility(), cryptoUtility, propertiesLoader), new PackageUtility(cryptoUtility, operatingSystem, propertiesLoader, new DownloadUtility()));
        serverCommunication.waitForServerAvailability();
        while (serverCommunication.sendUpdateRequest()) {
            logger.info("Checking for deployment");
        }
    }
}
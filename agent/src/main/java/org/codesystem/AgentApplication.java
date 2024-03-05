package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.enums.OperatingSystem;

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
    public static final PropertiesLoader properties = new PropertiesLoader();
    public static final Logger logger = Logger.getLogger("");
    public static String agentChecksum;
    public static OperatingSystem operatingSystem;

    public static void main(String[] args) {
        initialSetup();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(AgentApplication::mainLogic, 0, Integer.parseInt(properties.getProperty("Agent.Update-Interval")), TimeUnit.SECONDS);
    }

    private static void initialSetup() {
        String filename;
        try {
            filename = String.valueOf(new File(AgentApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot get filename");
        }
        if (filename.endsWith("Agent_update-download.jar")) {
            logger.info("Entering Update Mode");
            UpdateHandler updateHandler = new UpdateHandler();
            updateHandler.updateApplication();
        } else if (Files.exists(Paths.get("Agent_update-download.jar"))) {
            try {
                Files.delete(Paths.get("Agent_update-download.jar"));
            } catch (IOException e) {
                throw new RuntimeException("Cannot delete old update");
            }
        }
        HardwareInfo hardwareInfo = new HardwareInfo();
        if (!hardwareInfo.isElevated()) {
            throw new RuntimeException("Application needs elevated permissions");
        }

        if (hardwareInfo.getOsManufacturer().toLowerCase().contains("linux")) {
            AgentApplication.operatingSystem = OperatingSystem.LINUX;
        } else {
            throw new RuntimeException("Unsupported OS");
        }

        logger.info("Agent Startup");
        Security.addProvider(new BouncyCastleProvider());
        properties.loadProperties();

        CryptoHandler cryptoHandler = new CryptoHandler();
        agentChecksum = cryptoHandler.calculateChecksumOfFile("Agent.jar");

        ServerCommunicationRegistration serverCommunicationRegistration = new ServerCommunicationRegistration();
        serverCommunicationRegistration.validateRegistration();
    }

    private static void mainLogic() {
        logger.info("Checking for deployment");
        ServerCommunication serverCommunication = new ServerCommunication(operatingSystem);
        serverCommunication.waitForServerAvailability();
        while (serverCommunication.sendUpdateRequest()) {
            logger.info("Checking for deployment");
        }
    }
}
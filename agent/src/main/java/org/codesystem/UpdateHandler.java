package org.codesystem;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.EmptyRequest;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.utility.DownloadUtility;
import org.codesystem.utility.SystemExitUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateHandler {
    private final DownloadUtility downloadUtility;
    private final CryptoHandler cryptoHandler;
    private final PropertiesLoader propertiesLoader;
    String FILE_NAME_AGENT_UPDATE = "Agent_update.jar";
    Path PATH_UPDATE_FILE = Paths.get(FILE_NAME_AGENT_UPDATE);

    public UpdateHandler(DownloadUtility downloadUtility, CryptoHandler cryptoHandler, PropertiesLoader propertiesLoader) {
        this.downloadUtility = downloadUtility;
        this.cryptoHandler = cryptoHandler;
        this.propertiesLoader = propertiesLoader;
    }

    public void startUpdateProcess(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            throw new SevereAgentErrorException("Invalid Checksum");
        }
        try {
            Files.deleteIfExists(PATH_UPDATE_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot delete old update: " + e.getMessage());
        }

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoHandler), cryptoHandler, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty("Server.Url") + "/api/agent/communication/agent")
                .post(body)
                .build();

        if (!downloadUtility.downloadFile(PATH_UPDATE_FILE, request)) {
            throw new SevereAgentErrorException("Update-download from Server failed");
        }
        if (!cryptoHandler.calculateChecksumOfFile(FILE_NAME_AGENT_UPDATE).equals(checksum)) {
            try {
                Files.deleteIfExists(PATH_UPDATE_FILE);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete invalid Agent: " + e.getMessage());
            }
            throw new SevereAgentErrorException("Checksum of new Agent is invalid");
        }
        startNewApplication();
    }

    private void startNewApplication() {
        String command = ProcessHandle.current().info().commandLine().get();
        command = command.substring(0, command.indexOf(" "));
        AgentApplication.logger.info("Update command: " + command);
        try {
            new ProcessBuilder(command, "-jar", FILE_NAME_AGENT_UPDATE).start();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to execute update process: " + e.getMessage());
        }
        SystemExitUtility.exit(-10);
    }
}

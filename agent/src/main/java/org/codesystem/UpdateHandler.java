package org.codesystem;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.EmptyRequest;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.utility.DownloadUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateHandler {
    private static final String FILE_NAME_AGENT_UPDATE_DOWNLOAD = "Agent_update.jar";
    private static final Path PATH_DOWNLOAD_FILE = Paths.get(FILE_NAME_AGENT_UPDATE_DOWNLOAD);

    private final DownloadUtility downloadUtility;
    private final CryptoHandler cryptoHandler;
    private final PropertiesLoader propertiesLoader;

    public UpdateHandler(DownloadUtility downloadUtility, CryptoHandler cryptoHandler, PropertiesLoader propertiesLoader) {
        this.downloadUtility = downloadUtility;
        this.cryptoHandler = cryptoHandler;
        this.propertiesLoader = propertiesLoader;
    }

    public void updateApplication() {
        File oldVersion = Paths.get("Agent.jar").toFile();
        File backupOldVersion = Paths.get("Agent_backup.jar").toFile();
        File currentVersion = Paths.get(FILE_NAME_AGENT_UPDATE_DOWNLOAD).toFile();

        if (!currentVersion.exists() || !oldVersion.exists()) {
            throw new SevereAgentErrorException("Cannot find update file");
        }

        if (backupOldVersion.exists()) {
            try {
                Files.delete(backupOldVersion.toPath());
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old backup: " + e.getMessage());
            }
        }

        try {
            Files.copy(oldVersion.toPath(), backupOldVersion.toPath());
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot create backup: " + e.getMessage());
        }
        try {
            Files.delete(oldVersion.toPath());
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot delete old Agent: " + e.getMessage());
        }

        try {
            Files.copy(currentVersion.toPath(), oldVersion.toPath());
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot copy new Agent: " + e.getMessage());
        }
        SystemExit.exit(0);
    }

    public void startUpdateProcess(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            throw new SevereAgentErrorException("Invalid Checksum");
        }
        try {
            Files.deleteIfExists(PATH_DOWNLOAD_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot delete old update: " + e.getMessage());
        }

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoHandler), cryptoHandler, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty("Server.Url") + "/api/agent/communication/agent")
                .post(body)
                .build();

        downloadUtility.downloadFile(PATH_DOWNLOAD_FILE, request);
        if (!cryptoHandler.calculateChecksumOfFile(FILE_NAME_AGENT_UPDATE_DOWNLOAD).equals(checksum)) {
            try {
                Files.deleteIfExists(PATH_DOWNLOAD_FILE);
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
        try {
            new ProcessBuilder(command, "-jar", FILE_NAME_AGENT_UPDATE_DOWNLOAD).start();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to execute update process: " + e.getMessage());
        }
        SystemExit.exit(-10);
    }
}

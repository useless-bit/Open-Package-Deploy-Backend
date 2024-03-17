package org.codesystem;

import okhttp3.*;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.DetailedSystemInformation;
import org.codesystem.payload.EmptyRequest;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.UpdateCheckRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateHandler {
    private final PropertiesLoader propertiesLoader;
    private static final String FILE_NAME_AGENT_UPDATE_DOWNLOAD = "Agent_update-download.jar";

    public UpdateHandler(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public void updateApplication() {
        File oldVersion = Paths.get("Agent.jar").toFile();
        File backupOldVersion = Paths.get("Agent_backup.jar").toFile();
        File currentVersion = Paths.get(FILE_NAME_AGENT_UPDATE_DOWNLOAD).toFile();

        if (!currentVersion.exists()) {
            throw new SevereAgentErrorException("Cannot find update file");
        }

        if (backupOldVersion.exists()) {
            try {
                Files.delete(backupOldVersion.toPath());
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old backup: " + e.getMessage());
            }
        }

        if (oldVersion.exists()) {
            try {
                Files.copy(oldVersion.toPath(), backupOldVersion.toPath());
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot create backup");
            }
            try {
                Files.delete(oldVersion.toPath());
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old Agent: " + e.getMessage());
            }
        }

        try {
            Files.copy(currentVersion.toPath(), oldVersion.toPath());
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot copy new Agent");
        }
        System.exit(0);
    }

    public void startUpdateProcess(String checksum) {
        Path path = Paths.get(FILE_NAME_AGENT_UPDATE_DOWNLOAD);
        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete update: " + e.getMessage());
            }
        }

        CryptoHandler cryptoHandler = new CryptoHandler();
        downloadUpdate();
        if (!cryptoHandler.calculateChecksumOfFile(FILE_NAME_AGENT_UPDATE_DOWNLOAD).equals(checksum)) {
            throw new SevereAgentErrorException("Checksum of new Agent is invalid");
        }
        startNewApplication();
    }

    private void downloadUpdate() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(new CryptoHandler()), new CryptoHandler(), AgentApplication.properties).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/agent")
                .post(body)
                .build();

        Response response;
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to download update: " + e.getMessage());
        }
        if (response.code() != 200) {
            return;
        }

        byte[] data;
        try {
            data = response.body().bytes();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to load update from Server-response: " + e.getMessage());
        }
        try (FileOutputStream fos = new FileOutputStream(FILE_NAME_AGENT_UPDATE_DOWNLOAD)) {
            fos.write(data);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to write update file: " + e.getMessage());
        }

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

package org.codesystem;

import okhttp3.*;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.UpdateCheckRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UpdateHandler {
    public void updateApplication() {
        File oldVersion = Paths.get("Agent.jar").toFile();
        File backupOldVersion = Paths.get("Agent_backup.jar").toFile();
        File currentVersion = Paths.get("Agent_update-download.jar").toFile();

        if (!currentVersion.exists()) {
            throw new RuntimeException("Cannot find update");
        }

        if (backupOldVersion.exists()) {
            backupOldVersion.delete();
        }

        if (oldVersion.exists()) {
            try {
                Files.copy(oldVersion.toPath(), backupOldVersion.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Cannot create backup");
            }
            oldVersion.delete();
        }

        try {
            Files.copy(currentVersion.toPath(), oldVersion.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy update");
        }
        System.exit(0);
    }

    public void startUpdateProcess(String checksum) {
        try {
            Files.delete(Paths.get("Agent_update-download.jar"));
        } catch (IOException e) {

        }
        CryptoHandler cryptoHandler = new CryptoHandler();
        downloadUpdate();
        if (!cryptoHandler.calculateChecksumOfFile("Agent_update-download.jar").equals(checksum)) {
            System.exit(12);
        }
        startNewApplication();
    }

    private void downloadUpdate() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new UpdateCheckRequest().toJsonObject()).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/agent")
                .post(body)
                .build();

        Response response;
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response.code() != 200) {
            return;
        }

        byte[] data;
        try {
            data = response.body().bytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileOutputStream fos = new FileOutputStream("Agent_update-download.jar")) {
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void startNewApplication() {
        String command = ProcessHandle.current().info().commandLine().get();
        command = command.substring(0, command.indexOf(" "));
        try {
            new ProcessBuilder(command, "-jar", "Agent_update-download.jar").start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }
}

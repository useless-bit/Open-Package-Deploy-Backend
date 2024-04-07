package org.codesystem.utility;

import okhttp3.Request;
import org.codesystem.AgentApplication;
import org.codesystem.exceptions.SevereAgentErrorException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class DownloadUtility {
    /**
     * returns true if the file was successfully downloaded. Otherwise, false is returned.
     * Missing folders will be created.
     * If the target file is present, it will return false.
     */
    public boolean downloadFile(Path targetFileLocation, Request request) {
        if (targetFileLocation == null || request == null) {
            return false;
        }
        if (targetFileLocation.toFile().exists() || (targetFileLocation.toFile().getParentFile() != null && !targetFileLocation.toFile().getParentFile().exists() && !targetFileLocation.toFile().getParentFile().mkdirs())) {
            return false;
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(request.url().uri())
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try {
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new SevereAgentErrorException("Unable to download file: " + e.getMessage());

        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFileLocation.toFile())) {
            if (response.statusCode() != 200) {
                Files.deleteIfExists(targetFileLocation);
                AgentApplication.logger.log(Level.WARNING, "Error during download. Response Code: {0}", response.statusCode());
                return false;
            }
            byte[] data = response.body();
            fileOutputStream.write(data);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(targetFileLocation);
            } catch (IOException ex) {
                throw new SevereAgentErrorException("Unable to delete file: " + e.getMessage());
            }
            throw new SevereAgentErrorException("Unable to download file: " + e.getMessage());
        }
        return true;
    }
}

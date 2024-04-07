package org.codesystem.utility;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.codesystem.AgentApplication;
import org.codesystem.exceptions.DownloadException;

import java.io.IOException;
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

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute();
             BufferedSink bufferedSink = Okio.buffer(Okio.sink(targetFileLocation))) {
            if (response.code() != 200) {
                Files.deleteIfExists(targetFileLocation);
                AgentApplication.logger.log(Level.WARNING, "Error during download. Response Code: {0}", response.code());
                return false;
            }
            if (response.body() == null) {
                AgentApplication.logger.log(Level.WARNING, "Message Body is empty");
                return false;
            }
            bufferedSink.writeAll(response.body().source());
        } catch (IOException e) {
            try {
                Files.deleteIfExists(targetFileLocation);
            } catch (IOException ex) {
                throw new DownloadException("Unable to delete file: " + e.getMessage());
            }
            throw new DownloadException("Unable to download file: " + e.getMessage());
        }
        return true;
    }
}

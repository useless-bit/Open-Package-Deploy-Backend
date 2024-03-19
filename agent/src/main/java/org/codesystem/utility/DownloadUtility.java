package org.codesystem.utility;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.codesystem.exceptions.SevereAgentErrorException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        if (targetFileLocation.toFile().exists() || !targetFileLocation.toFile().getParentFile().mkdirs()) {
            return false;
        }

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute();
             FileOutputStream fileOutputStream = new FileOutputStream(targetFileLocation.toString())) {
            if (response.code() != 200) {
                Files.deleteIfExists(targetFileLocation);
                return false;
            }
            byte[] data = response.body().bytes();
            fileOutputStream.write(data);
        } catch (IOException e) {
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

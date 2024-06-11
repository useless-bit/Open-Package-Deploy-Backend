package org.codesystem;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.EmptyRequest;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.DownloadUtility;
import org.codesystem.utility.SystemExitUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateHandler {
    private static final Path PATH_UPDATE_FILE = Paths.get(Variables.FILE_NAME_AGENT_UPDATE);
    private final DownloadUtility downloadUtility;
    private final CryptoUtility cryptoUtility;
    private final PropertiesLoader propertiesLoader;

    public UpdateHandler(DownloadUtility downloadUtility, CryptoUtility cryptoUtility, PropertiesLoader propertiesLoader) {
        this.downloadUtility = downloadUtility;
        this.cryptoUtility = cryptoUtility;
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
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoUtility), cryptoUtility, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_URL) + Variables.URL_AGENT_DOWNLOAD)
                .post(body)
                .build();

        boolean updateDownloaded = false;
        try {
            updateDownloaded = downloadUtility.downloadFile(PATH_UPDATE_FILE, request);
        } catch (Exception e) {
            throw new SevereAgentErrorException("Update-download from Server failed: " + e.getMessage());
        }
        if (!updateDownloaded) {
            throw new SevereAgentErrorException("Update-download from Server failed");
        }
        if (!cryptoUtility.calculateChecksumOfFile(Variables.FILE_NAME_AGENT_UPDATE).equals(checksum)) {
            try {
                Files.deleteIfExists(PATH_UPDATE_FILE);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete invalid Agent: " + e.getMessage());
            }
            throw new SevereAgentErrorException("Checksum of new Agent is invalid");
        }
        SystemExitUtility.exit(0);
    }
}

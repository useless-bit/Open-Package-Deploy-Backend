package org.codesystem;

import okhttp3.*;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.UpdateCheckRequest;
import org.codesystem.payload.UpdateCheckResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class ServerCommunication {
    private final CryptoHandler cryptoHandler;
    private final OperatingSystem operatingSystem;

    public ServerCommunication(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        this.cryptoHandler = new CryptoHandler();
    }

    private boolean isServerAvailable() {
        URL serverUrl;
        try {
            serverUrl = new URL(AgentApplication.properties.getProperty("Server.Url"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error when parsing the Server URL: '" + e.getMessage() + "'");
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(serverUrl + "/monitoring/health").build();
        Response response;
        try {
            response = client.newCall(request).execute();
            response.close();
        } catch (IOException e) {
            return false;
        }
        return response.code() == 200;
    }

    public void waitForServerAvailability() {
        AgentApplication.logger.info("Check if server is available");
        while (!isServerAvailable()) {
            AgentApplication.logger.info("Server not available. Pausing thread for 10 seconds.");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                AgentApplication.logger.severe("Cannot pause thread");
            }
        }
        AgentApplication.logger.info("Server is available");

    }

    public boolean sendUpdateRequest() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new UpdateCheckRequest().toJsonObject()).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/checkForUpdates")
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
            return false;
        }
        String responseBody;
        try {
            responseBody = new JSONObject(response.body().string()).getString("message");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String decrypted = cryptoHandler.decryptECC(Base64.getDecoder().decode(responseBody.getBytes(StandardCharsets.UTF_8)));
        UpdateCheckResponse updateCheckResponse = new UpdateCheckResponse(new JSONObject(decrypted));

        AgentApplication.logger.info("Server: " + updateCheckResponse.getAgentChecksum());
        AgentApplication.logger.info("Local: " + AgentApplication.agentChecksum);
        if (!updateCheckResponse.getAgentChecksum().equals(AgentApplication.agentChecksum)) {
            AgentApplication.logger.info("Initiate update");
            UpdateHandler updateHandler = new UpdateHandler();
            updateHandler.startUpdateProcess(updateCheckResponse.getAgentChecksum());
        }

        if (Integer.parseInt(AgentApplication.properties.getProperty("Agent.Update-Interval")) != updateCheckResponse.getUpdateCheckTimeout()) {
            AgentApplication.properties.setProperty("Agent.Update-Interval", String.valueOf(updateCheckResponse.getUpdateCheckTimeout()));
            AgentApplication.properties.saveProperties();
            System.exit(0);
        }

        if (updateCheckResponse.isDeploymentAvailable()) {
            AgentApplication.logger.info("Deployment Found");
            PackageHandler packageHandler = new PackageHandler(operatingSystem);
            packageHandler.initiateDeployment();
            return true;
        }
        return false;
    }
}

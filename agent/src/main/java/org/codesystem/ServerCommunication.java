package org.codesystem;

import okhttp3.*;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.exceptions.SevereAgentErrorException;
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
    private final PropertiesLoader propertiesLoader;
    private final String agentChecksum;
    private final UpdateHandler updateHandler;
    private final PackageHandler packageHandler;

    public ServerCommunication(CryptoHandler cryptoHandler, PropertiesLoader propertiesLoader, String agentChecksum, UpdateHandler updateHandler, PackageHandler packageHandler) {
        this.cryptoHandler = cryptoHandler;
        this.propertiesLoader = propertiesLoader;
        this.agentChecksum = agentChecksum;
        this.updateHandler = updateHandler;
        this.packageHandler = packageHandler;
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
        RequestBody body = RequestBody.create(new EncryptedMessage(new UpdateCheckRequest(cryptoHandler).toJsonObject(), cryptoHandler, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty("Server.Url") + "/api/agent/communication/checkForUpdates")
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                return false;
            }
            String responseBody = new JSONObject(response.body().string()).getString("message");
            String decrypted = cryptoHandler.decryptECC(Base64.getDecoder().decode(responseBody.getBytes(StandardCharsets.UTF_8)));
            UpdateCheckResponse updateCheckResponse = new UpdateCheckResponse(new JSONObject(decrypted));
            return processUpdateCheckResponse(updateCheckResponse);
        } catch (Exception e) {
            throw new SevereAgentErrorException(e.getMessage());
        }


    }

    public boolean processUpdateCheckResponse(UpdateCheckResponse updateCheckResponse) {
        if (updateCheckResponse == null) {
            return false;
        }
        if (!updateCheckResponse.getAgentChecksum().isBlank() && !updateCheckResponse.getAgentChecksum().equals(agentChecksum)) {
            AgentApplication.logger.info("Initiate update");
            updateHandler.startUpdateProcess(updateCheckResponse.getAgentChecksum());
        }

        if (Integer.parseInt(propertiesLoader.getProperty("Agent.Update-Interval")) != updateCheckResponse.getUpdateInterval() && updateCheckResponse.getUpdateInterval() >= 1) {
            propertiesLoader.setProperty("Agent.Update-Interval", String.valueOf(updateCheckResponse.getUpdateInterval()));
            propertiesLoader.saveProperties();
            SystemExit.exit(-10);
        }

        if (updateCheckResponse.isDeploymentAvailable()) {
            AgentApplication.logger.info("Deployment Found");
            packageHandler.initiateDeployment();
            return true;
        }
        return false;
    }
}

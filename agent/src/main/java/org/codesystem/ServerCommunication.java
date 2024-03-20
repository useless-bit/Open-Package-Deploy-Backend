package org.codesystem;

import okhttp3.*;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.DetailedSystemInformation;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.UpdateCheckRequest;
import org.codesystem.payload.UpdateCheckResponse;
import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.PackageUtility;
import org.codesystem.utility.SystemExitUtility;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class ServerCommunication {
    private final CryptoUtility cryptoUtility;
    private final PropertiesLoader propertiesLoader;
    private final String agentChecksum;
    private final UpdateHandler updateHandler;
    private final PackageUtility packageUtility;

    public ServerCommunication(CryptoUtility cryptoUtility, PropertiesLoader propertiesLoader, String agentChecksum, UpdateHandler updateHandler, PackageUtility packageUtility) {
        this.cryptoUtility = cryptoUtility;
        this.propertiesLoader = propertiesLoader;
        this.agentChecksum = agentChecksum;
        this.updateHandler = updateHandler;
        this.packageUtility = packageUtility;
    }

    private boolean isServerAvailable() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(propertiesLoader.getProperty("Server.Url") + "/monitoring/health").build();
        Response response;
        try {
            response = client.newCall(request).execute();
            response.close();
        } catch (Exception e) {
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
                Thread.currentThread().interrupt();
            }
        }
        AgentApplication.logger.info("Server is available");
    }

    public boolean sendUpdateRequest() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new UpdateCheckRequest(agentChecksum, new DetailedSystemInformation(new HardwareInfo())).toJsonObject(cryptoUtility), cryptoUtility, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty("Server.Url") + Variables.URL_UPDATE_CHECK_REQUEST)
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                AgentApplication.logger.info("HTTP response-code: " + response.code());
                return false;
            }
            String responseBody = new JSONObject(response.body().string()).getString("message");
            String decrypted = cryptoUtility.decryptECC(Base64.getDecoder().decode(responseBody.getBytes(StandardCharsets.UTF_8)));
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
        if (updateCheckResponse.getAgentChecksum() != null && !updateCheckResponse.getAgentChecksum().isBlank() && !updateCheckResponse.getAgentChecksum().equals(agentChecksum)) {
            AgentApplication.logger.info("Initiate update");
            updateHandler.startUpdateProcess(updateCheckResponse.getAgentChecksum());
        }

        if (Integer.parseInt(propertiesLoader.getProperty("Agent.Update-Interval")) != updateCheckResponse.getUpdateInterval() && updateCheckResponse.getUpdateInterval() >= 1) {
            propertiesLoader.setProperty("Agent.Update-Interval", String.valueOf(updateCheckResponse.getUpdateInterval()));
            propertiesLoader.saveProperties();
            SystemExitUtility.exit(-10);
        }

        if (updateCheckResponse.isDeploymentAvailable()) {
            AgentApplication.logger.info("Deployment Found");
            packageUtility.initiateDeployment();
            return true;
        }
        return false;
    }
}

package org.codesystem;

import okhttp3.*;
import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.PackageUtility;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ServerCommunicationRegistration {
    private final CryptoUtility cryptoUtility;
    private final PropertiesLoader propertiesLoader;
    private final ServerCommunication serverCommunication;
    private final String agentChecksum;
    private final UpdateHandler updateHandler;
    private final PackageUtility packageUtility;

    public ServerCommunicationRegistration(CryptoUtility cryptoUtility, PropertiesLoader propertiesLoader, String agentChecksum, UpdateHandler updateHandler, PackageUtility packageUtility) {
        this.cryptoUtility = cryptoUtility;
        this.propertiesLoader = propertiesLoader;
        this.agentChecksum = agentChecksum;
        this.updateHandler = updateHandler;
        this.packageUtility = packageUtility;
        this.serverCommunication = new ServerCommunication(cryptoUtility, propertiesLoader, agentChecksum, updateHandler, packageUtility);
    }

    public void validateRegistration() {
        AgentApplication.logger.info("Checking if Server is available");
        serverCommunication.waitForServerAvailability();
        AgentApplication.logger.info("The Server is available. Staring registration");

        //clear server public key when not registered
        if (propertiesLoader.getProperty("Server.Registered").equals("false")) {
            propertiesLoader.setProperty("Server.ECC.Public-Key", "");
            propertiesLoader.saveProperties();
        }

        //register on server
        if (propertiesLoader.getProperty("Server.ECC.Public-Key").isBlank()) {
            AgentApplication.logger.info("No Server Public Key found. Registering on Server.");
            if (propertiesLoader.getProperty("Server.Registration-Token") == null || propertiesLoader.getProperty("Server.Registration-Token").isBlank()) {
                propertiesLoader.setProperty("Server.Registration-Token", "");
                propertiesLoader.saveProperties();
                throw new RuntimeException("Cannot register without a Token. Set the 'Server.Registration-Token'");
            }
            registerOnServer();
            AgentApplication.logger.info("Registered on Server.");
        }
    }

    private void registerOnServer() {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        JSONObject jsonRequestBody = new JSONObject()
                .put("publicKeyBase64", propertiesLoader.getProperty("Agent.ECC.Public-Key"))
                .put("name", inetAddress.getCanonicalHostName())
                .put("authenticationToken", propertiesLoader.getProperty("Server.Registration-Token"));

        RequestBody body = RequestBody.create(jsonRequestBody.toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty("Server.Url") + "/api/agent/registration")
                .post(body)
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response.code() != 200) {
            throw new RuntimeException("Cannot register on Server. Response: " + response);
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = new JSONObject(response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        propertiesLoader.setProperty("Server.ECC.Public-Key", jsonResponse.get("publicKeyBase64").toString());
        propertiesLoader.saveProperties();


        String verificationToken = cryptoUtility.decryptECC(Base64.getDecoder().decode(jsonResponse.get("encryptedValidationToken").toString()));

        verificationToken = Base64.getEncoder().encodeToString(cryptoUtility.encryptECC(verificationToken.getBytes(StandardCharsets.UTF_8)));

        jsonRequestBody = new JSONObject()
                .put("publicKeyBase64", propertiesLoader.getProperty("Agent.ECC.Public-Key"))
                .put("verificationToken", verificationToken);

        body = RequestBody.create(jsonRequestBody.toString(), mediaType);
        request = new Request.Builder()
                .url(propertiesLoader.getProperty("Server.Url") + "/api/agent/registration/verify")
                .post(body)
                .build();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response.code() != 200) {
            throw new RuntimeException("Cannot register on Server. Response: " + response);
        }

        propertiesLoader.setProperty("Server.Registered", "true");
        propertiesLoader.remove("Server.Registration-Token");
        propertiesLoader.saveProperties();

    }


}

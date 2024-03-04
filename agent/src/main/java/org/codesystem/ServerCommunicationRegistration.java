package org.codesystem;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ServerCommunicationRegistration {
    private final ServerCommunication serverCommunication = new ServerCommunication(AgentApplication.operatingSystem);

    public void validateRegistration() {
        AgentApplication.logger.info("Checking if Server is available");
        serverCommunication.waitForServerAvailability();
        AgentApplication.logger.info("The Server is available. Staring registration");

        //clear server public key when not registered
        if (AgentApplication.properties.getProperty("Server.Registered").equals("false")) {
            AgentApplication.properties.setProperty("Server.ECC.Public-Key", "");
            AgentApplication.properties.saveProperties();
        }

        //register on server
        if (AgentApplication.properties.getProperty("Server.ECC.Public-Key").isBlank()) {
            AgentApplication.logger.info("No Server Public Key found. Registering on Server.");
            if (AgentApplication.properties.getProperty("Server.Registration-Token") == null || AgentApplication.properties.getProperty("Server.Registration-Token").isBlank()) {
                AgentApplication.properties.setProperty("Server.Registration-Token", "");
                AgentApplication.properties.saveProperties();
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
                .put("publicKeyBase64", AgentApplication.properties.getProperty("Agent.ECC.Public-Key"))
                .put("name", inetAddress.getCanonicalHostName())
                .put("authenticationToken", AgentApplication.properties.getProperty("Server.Registration-Token"));

        RequestBody body = RequestBody.create(jsonRequestBody.toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/registration")
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
        CryptoHandler cryptoHandler = new CryptoHandler();
        AgentApplication.properties.setProperty("Server.ECC.Public-Key", jsonResponse.get("publicKeyBase64").toString());
        AgentApplication.properties.saveProperties();


        String verificationToken = cryptoHandler.decryptECC(Base64.getDecoder().decode(jsonResponse.get("encryptedValidationToken").toString()));

        cryptoHandler = new CryptoHandler();
        verificationToken = Base64.getEncoder().encodeToString(cryptoHandler.encryptECC(verificationToken.getBytes(StandardCharsets.UTF_8)));

        jsonRequestBody = new JSONObject()
                .put("publicKeyBase64", AgentApplication.properties.getProperty("Agent.ECC.Public-Key"))
                .put("verificationToken", verificationToken);

        body = RequestBody.create(jsonRequestBody.toString(), mediaType);
        request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/registration/verify")
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

        AgentApplication.properties.setProperty("Server.Registered", "true");
        AgentApplication.properties.remove("Server.Registration-Token");
        AgentApplication.properties.saveProperties();

    }


}

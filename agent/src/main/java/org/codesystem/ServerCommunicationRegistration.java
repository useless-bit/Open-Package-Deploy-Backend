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

    public ServerCommunicationRegistration(CryptoUtility cryptoUtility, PropertiesLoader propertiesLoader, String agentChecksum, UpdateHandler updateHandler, PackageUtility packageUtility) {
        this.cryptoUtility = cryptoUtility;
        this.propertiesLoader = propertiesLoader;
        this.serverCommunication = new ServerCommunication(cryptoUtility, propertiesLoader, agentChecksum, updateHandler, packageUtility);
    }

    public void validateRegistration() {
        AgentApplication.logger.info("Checking if Server is available");
        serverCommunication.waitForServerAvailability();
        AgentApplication.logger.info("The Server is available. Staring registration");

        //clear server public key when not registered
        if (propertiesLoader.getProperty("Server.Registered").equals("false")) {
            propertiesLoader.setProperty(Variables.PROPERTIES_SERVER_ECC_PUBLIC_KEY, "");
            propertiesLoader.saveProperties();
        }

        //register on server
        if (propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_ECC_PUBLIC_KEY).isBlank()) {
            AgentApplication.logger.info("No Server Public Key found. Registering on Server.");
            if (propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_REGISTRATION_TOKEN) == null || propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_REGISTRATION_TOKEN).isBlank()) {
                propertiesLoader.setProperty(Variables.PROPERTIES_SERVER_REGISTRATION_TOKEN, "");
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
                .put("authenticationToken", propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_REGISTRATION_TOKEN));

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
        propertiesLoader.setProperty(Variables.PROPERTIES_SERVER_ECC_PUBLIC_KEY, jsonResponse.get("publicKeyBase64").toString());
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
        propertiesLoader.remove(Variables.PROPERTIES_SERVER_REGISTRATION_TOKEN);
        propertiesLoader.saveProperties();

    }


}

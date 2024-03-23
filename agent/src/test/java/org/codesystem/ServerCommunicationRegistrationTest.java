package org.codesystem;

import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.SystemExitUtility;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockserver.model.HttpRequest.request;

class ServerCommunicationRegistrationTest {
    ServerCommunicationRegistration serverCommunicationRegistration;
    PropertiesLoader propertiesLoader;
    CryptoUtility cryptoUtility;
    ServerCommunication serverCommunication;
    ClientAndServer mockServer;
    MockedStatic<SystemExitUtility> systemExitMockedStatic;

    @BeforeEach
    void setUp() {
        propertiesLoader = Mockito.mock(PropertiesLoader.class);
        Mockito.when(propertiesLoader.getProperty("Server.Url")).thenReturn("http://localhost:8899");
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("PublicKey");
        Mockito.when(propertiesLoader.getProperty("Agent.Update-Interval")).thenReturn("10");
        Mockito.when(propertiesLoader.getProperty("Server.Registered")).thenReturn("false");
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn("");
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        Mockito.when(cryptoUtility.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes());
        Mockito.when(cryptoUtility.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes());
        serverCommunication = Mockito.mock(ServerCommunication.class);
        Mockito.doNothing().when(serverCommunication).waitForServerAvailability();
        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);
        mockServer = ClientAndServer.startClientAndServer(8899);
        serverCommunicationRegistration = new ServerCommunicationRegistration(cryptoUtility, propertiesLoader, serverCommunication);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
        systemExitMockedStatic.close();
    }

    @Test
    void validateRegistration_registrationTokenMissing() {
        Assertions.assertThrows(TestSystemExitException.class, () -> serverCommunicationRegistration.validateRegistration());
        Mockito.when(propertiesLoader.getProperty("Server.Registration-Token")).thenReturn("");
        Assertions.assertThrows(TestSystemExitException.class, () -> serverCommunicationRegistration.validateRegistration());
        Mockito.when(propertiesLoader.getProperty("Server.Registration-Token")).thenReturn("   ");
        Assertions.assertThrows(TestSystemExitException.class, () -> serverCommunicationRegistration.validateRegistration());
    }

    @Test
    void validateRegistration_registrationTokenProvidedServerNotAvailable() {
        Mockito.when(propertiesLoader.getProperty("Server.Registration-Token")).thenReturn("Registration Token");
        Assertions.assertThrows(TestSystemExitException.class, () -> serverCommunicationRegistration.validateRegistration());
    }

    @Test
    void validateRegistration_registrationTokenProvidedServerAvailable_StageOne() {
        Mockito.when(propertiesLoader.getProperty("Server.Registration-Token")).thenReturn("Registration Token");
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "Server Public Key").put("encryptedValidationToken", Base64.getEncoder().encodeToString("Verification Code".getBytes(StandardCharsets.UTF_8)));
        mockServer.when(request().withMethod("POST").withPath("/api/agent/registration")).respond(HttpResponse.response().withStatusCode(200).withBody(jsonObject.toString()));
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn("Verification Token");
        Assertions.assertThrows(TestSystemExitException.class, () -> serverCommunicationRegistration.validateRegistration());
    }

    @Test
    void validateRegistration_valid() {
        Mockito.when(propertiesLoader.getProperty("Server.Registration-Token")).thenReturn("Registration Token");
        JSONObject jsonObject = new JSONObject().put("publicKeyBase64", "Server Public Key").put("encryptedValidationToken", Base64.getEncoder().encodeToString("Verification Code".getBytes(StandardCharsets.UTF_8)));
        mockServer.when(request().withMethod("POST").withPath("/api/agent/registration")).respond(HttpResponse.response().withStatusCode(200).withBody(jsonObject.toString()));
        mockServer.when(request().withMethod("POST").withPath("/api/agent/registration/verify")).respond(HttpResponse.response().withStatusCode(200).withBody(jsonObject.toString()));
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn("Verification Token");
        serverCommunicationRegistration.validateRegistration();
        Assertions.assertDoesNotThrow(() -> serverCommunicationRegistration.validateRegistration());
    }
}
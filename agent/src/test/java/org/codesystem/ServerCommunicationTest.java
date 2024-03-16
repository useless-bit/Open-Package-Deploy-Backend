package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.security.Security;

import static org.mockserver.model.HttpRequest.request;

class ServerCommunicationTest {
    ServerCommunication serverCommunication;
    PropertiesLoader propertiesLoader;
    CryptoHandler cryptoHandler;
    ClientAndServer mockServer;

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
        propertiesLoader = Mockito.mock(PropertiesLoader.class);
        Mockito.when(propertiesLoader.getProperty("Server.Url")).thenReturn("http://localhost:8899");
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("PublicKey");
        cryptoHandler = Mockito.mock(CryptoHandler.class);
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes());
        Mockito.when(cryptoHandler.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes());
        serverCommunication = new ServerCommunication(OperatingSystem.LINUX, cryptoHandler, propertiesLoader);
        mockServer = ClientAndServer.startClientAndServer(8899);
    }

    @Test
    void sendUpdateRequest() {
        // invalid return value
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/checkForUpdates")).respond(HttpResponse.response().withStatusCode(500));
        Assertions.assertFalse(serverCommunication.sendUpdateRequest());
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/checkForUpdates")).respond(HttpResponse.response().withStatusCode(400));
        Assertions.assertFalse(serverCommunication.sendUpdateRequest());

        // invalid response
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/checkForUpdates")).respond(HttpResponse.response().withStatusCode(200));
        Assertions.assertThrows(SevereAgentErrorException.class, () -> serverCommunication.sendUpdateRequest());

        // invalid response
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/checkForUpdates")).respond(HttpResponse.response().withStatusCode(200));
        Assertions.assertThrows(SevereAgentErrorException.class, () -> serverCommunication.sendUpdateRequest());
    }

}
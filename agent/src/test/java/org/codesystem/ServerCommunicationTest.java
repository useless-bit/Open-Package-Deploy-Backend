package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.UpdateCheckResponse;
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
import java.security.Security;
import java.util.Base64;

import static org.mockserver.model.HttpRequest.request;

class ServerCommunicationTest {
    ServerCommunication serverCommunication;
    PropertiesLoader propertiesLoader;
    CryptoHandler cryptoHandler;
    UpdateHandler updateHandler;
    ClientAndServer mockServer;

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
        propertiesLoader = Mockito.mock(PropertiesLoader.class);
        Mockito.when(propertiesLoader.getProperty("Server.Url")).thenReturn("http://localhost:8899");
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("PublicKey");
        Mockito.when(propertiesLoader.getProperty("Agent.Update-Interval")).thenReturn("10");
        cryptoHandler = Mockito.mock(CryptoHandler.class);
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes());
        Mockito.when(cryptoHandler.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes());
        updateHandler = Mockito.mock(UpdateHandler.class);
        mockServer = ClientAndServer.startClientAndServer(8899);
        serverCommunication = new ServerCommunication(OperatingSystem.LINUX, cryptoHandler, propertiesLoader, "agentChecksum", updateHandler);
        MockedStatic<SystemExit> systemExitMockedStatic = Mockito.mockStatic(SystemExit.class);
        systemExitMockedStatic.when(() -> SystemExit.exit(Mockito.anyInt())).thenThrow(TestException.class);
    }

    @AfterEach
    void teardown() {
        mockServer.stop();
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

        // valid response
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        JSONObject jsonObject = new JSONObject().put("updateInterval", "10").put("deploymentAvailable", "false").put("agentChecksum", "agentChecksum");
        Mockito.when(cryptoHandler.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/checkForUpdates")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertFalse(serverCommunication.sendUpdateRequest());
    }

    @Test
    void processUpdateCheckResponse() {
        // null values
        Assertions.assertFalse(serverCommunication.processUpdateCheckResponse(null));

        // invalid checksum
        JSONObject jsonObject = new JSONObject().put("updateInterval", "10").put("deploymentAvailable", "false").put("agentChecksum", "");
        Assertions.assertFalse(serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject)));
        jsonObject = new JSONObject().put("updateInterval", "10").put("deploymentAvailable", "false").put("agentChecksum", "   ");
        Assertions.assertFalse(serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject)));

        // invalid updateInterval
        jsonObject = new JSONObject().put("updateInterval", "-1").put("deploymentAvailable", "false").put("agentChecksum", "agentChecksum");
        Assertions.assertFalse(serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject)));
        jsonObject = new JSONObject().put("updateInterval", "0").put("deploymentAvailable", "false").put("agentChecksum", "agentChecksum");
        Assertions.assertFalse(serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject)));

        // checksum
        jsonObject = new JSONObject().put("updateInterval", "10").put("deploymentAvailable", "false").put("agentChecksum", "agentChecksum");
        Assertions.assertFalse(serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject)));
        jsonObject = new JSONObject().put("updateInterval", "10").put("deploymentAvailable", "false").put("agentChecksum", "differentChecksum");
        serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject));
        Mockito.verify(updateHandler).startUpdateProcess(Mockito.any());

        // updateInterval
        Assertions.assertThrows(TestException.class, () -> serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(new JSONObject().put("updateInterval", "20").put("deploymentAvailable", "false").put("agentChecksum", "agentChecksum"))));

    }


}
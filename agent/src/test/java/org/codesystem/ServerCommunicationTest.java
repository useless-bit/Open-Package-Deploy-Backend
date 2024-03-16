package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.util.concurrent.TimeUnit;

import static org.mockserver.model.HttpRequest.request;

class ServerCommunicationTest {
    ServerCommunication serverCommunication;
    PropertiesLoader propertiesLoader;
    CryptoHandler cryptoHandler;
    UpdateHandler updateHandler;
    PackageHandler packageHandler;
    ClientAndServer mockServer;
    MockedStatic<SystemExit> systemExitMockedStatic;

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
        packageHandler = Mockito.mock(PackageHandler.class);
        serverCommunication = new ServerCommunication(cryptoHandler, propertiesLoader, "agentChecksum", updateHandler, packageHandler);
        systemExitMockedStatic = Mockito.mockStatic(SystemExit.class);
        systemExitMockedStatic.when(() -> SystemExit.exit(Mockito.anyInt())).thenThrow(TestException.class);
        mockServer = ClientAndServer.startClientAndServer(8899);
    }

    @AfterEach
    void teardown() {
        mockServer.stop();
        systemExitMockedStatic.close();
    }

    @Test
    void waitForServerAvailability() {
        // available
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/monitoring/health")).respond(HttpResponse.response().withStatusCode(200));
        Assertions.assertDoesNotThrow(() -> serverCommunication.waitForServerAvailability());

        // not available
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/monitoring/health")).respond(HttpResponse.response().withStatusCode(500));
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            mockServer.stop();
            mockServer = ClientAndServer.startClientAndServer(8899);
            mockServer.when(request().withMethod("GET").withPath("/monitoring/health")).respond(HttpResponse.response().withStatusCode(200));
        }).start();
        Assertions.assertDoesNotThrow(() -> serverCommunication.waitForServerAvailability());
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

        // deployment
        jsonObject = new JSONObject().put("updateInterval", "10").put("deploymentAvailable", "true").put("agentChecksum", "agentChecksum");
        Assertions.assertTrue(serverCommunication.processUpdateCheckResponse(new UpdateCheckResponse(jsonObject)));

    }


}
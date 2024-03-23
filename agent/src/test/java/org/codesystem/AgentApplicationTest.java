package org.codesystem;

import org.codesystem.utility.CryptoUtility;
import org.codesystem.utility.SystemExitUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.mockserver.model.HttpRequest.request;

class AgentApplicationTest {
    AgentApplication agentApplication;
    PropertiesLoader propertiesLoader;
    HardwareInfo hardwareInfo;
    CryptoUtility cryptoUtility;
    ClientAndServer mockServer;
    MockedStatic<SystemExitUtility> systemExitMockedStatic;
    File updateFile = Paths.get("Agent_update.jar").toFile();

    @BeforeEach
    void setUp() {
        propertiesLoader = Mockito.mock(PropertiesLoader.class);
        hardwareInfo = Mockito.mock(HardwareInfo.class);
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        agentApplication = new AgentApplication(propertiesLoader, hardwareInfo, cryptoUtility);

        Mockito.when(propertiesLoader.getProperty("Server.Url")).thenReturn("http://localhost:8899");
        Mockito.when(propertiesLoader.getProperty("Server.Registered")).thenReturn("true");
        Mockito.when(propertiesLoader.getProperty("Server.ECC.Public-Key")).thenReturn("Server Public Key");
        Mockito.when(hardwareInfo.isElevated()).thenReturn(true);

        mockServer = ClientAndServer.startClientAndServer(8899);
        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);

        updateFile.delete();
    }

    @AfterEach
    void tearDown() {
        updateFile.delete();
        mockServer.stop();
        systemExitMockedStatic.close();
    }

    @Test
    void initialSetup_noUpdatePermissionIssue() {
        Mockito.when(hardwareInfo.isElevated()).thenReturn(false);
        Assertions.assertThrows(TestSystemExitException.class, () -> agentApplication.agentLogic());
    }

    @Test
    void initialSetup_invalidOS() {
        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("Invalid OS");
        Assertions.assertThrows(TestSystemExitException.class, () -> agentApplication.agentLogic());
    }

    @Test
    void initialSetup_linux() {
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/monitoring/health")).respond(HttpResponse.response().withStatusCode(200));

        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("Linux");
        Assertions.assertThrows(NumberFormatException.class, () -> agentApplication.agentLogic());
    }

    @Test
    void initialSetup_updateFilePresent() throws IOException {
        updateFile.createNewFile();
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/monitoring/health")).respond(HttpResponse.response().withStatusCode(200));

        Mockito.when(hardwareInfo.getOsManufacturer()).thenReturn("Linux");
        Assertions.assertThrows(NumberFormatException.class, () -> agentApplication.agentLogic());
        Assertions.assertFalse(updateFile.exists());
    }

}
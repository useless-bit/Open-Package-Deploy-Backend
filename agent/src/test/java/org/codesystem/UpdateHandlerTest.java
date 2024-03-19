package org.codesystem;

import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.utility.DownloadUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class UpdateHandlerTest {
    String FILE_NAME_AGENT_UPDATE = "Agent_update.jar";
    Path PATH_UPDATE_FILE = Paths.get(FILE_NAME_AGENT_UPDATE);
    String FILE_NAME_AGENT_UPDATE_BACKUP = "Agent_backup.jar";
    Path PATH_BACKUP_FILE = Paths.get(FILE_NAME_AGENT_UPDATE_BACKUP);
    String FILE_NAME_AGENT = "Agent.jar";
    Path PATH_FILE = Paths.get(FILE_NAME_AGENT);

    UpdateHandler updateHandler;
    DownloadUtility downloadUtility;
    CryptoHandler cryptoHandler;
    PropertiesLoader propertiesLoader;
    ClientAndServer mockServer;
    MockedStatic<SystemExit> systemExitMockedStatic;

    @BeforeEach
    void setUp() {
        downloadUtility = Mockito.mock(DownloadUtility.class);
        cryptoHandler = Mockito.mock(CryptoHandler.class);
        propertiesLoader = Mockito.mock(PropertiesLoader.class);

        Mockito.when(propertiesLoader.getProperty("Server.Url")).thenReturn("http://localhost:8899");
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("PublicKey");
        Mockito.when(cryptoHandler.createSignatureECC(Mockito.any())).thenReturn("signature".getBytes(StandardCharsets.UTF_8));
        Mockito.when(cryptoHandler.encryptECC(Mockito.any())).thenReturn("encrypted".getBytes(StandardCharsets.UTF_8));

        updateHandler = new UpdateHandler(downloadUtility, cryptoHandler, propertiesLoader);

        mockServer = ClientAndServer.startClientAndServer(8899);

        systemExitMockedStatic = Mockito.mockStatic(SystemExit.class);
        systemExitMockedStatic.when(() -> SystemExit.exit(Mockito.anyInt())).then(invocationOnMock -> null);
        deleteFiles();
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
        systemExitMockedStatic.close();
        deleteFiles();
    }


    private void deleteFiles() {
        PATH_UPDATE_FILE.toFile().delete();
        PATH_BACKUP_FILE.toFile().delete();
        PATH_FILE.toFile().delete();
    }


    @Test
    void updateApplication_noFilesPresent() {
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.updateApplication());
    }

    @Test
    void updateApplication_agentPresentUpdateMissing() throws IOException {
        Files.writeString(PATH_FILE, "Agent_File_Content");
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.updateApplication());
    }

    @Test
    void updateApplication_agentMissingUpdatePresent() throws IOException {
        Files.writeString(PATH_UPDATE_FILE, "Agent_Update_File_Content");
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.updateApplication());
    }

    @Test
    void updateApplication_agentPresentUpdatePresent() throws IOException {
        Files.writeString(PATH_FILE, "Agent_File_Content");
        Files.writeString(PATH_UPDATE_FILE, "Agent_Update_File_Content");
        Assertions.assertDoesNotThrow(() -> updateHandler.updateApplication());
        Assertions.assertTrue(PATH_FILE.toFile().exists());
        Assertions.assertTrue(PATH_BACKUP_FILE.toFile().exists());
        Assertions.assertTrue(PATH_UPDATE_FILE.toFile().exists());
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_FILE), "Agent_Update_File_Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_BACKUP_FILE), "Agent_File_Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_UPDATE_FILE), "Agent_Update_File_Content".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void updateApplication_allFilesPresent() throws IOException {
        Files.writeString(PATH_FILE, "Agent_File_Content");
        Files.writeString(PATH_BACKUP_FILE, "Agent_Backup_Content");
        Files.writeString(PATH_UPDATE_FILE, "Agent_Update_File_Content");
        Assertions.assertDoesNotThrow(() -> updateHandler.updateApplication());
        Assertions.assertTrue(PATH_FILE.toFile().exists());
        Assertions.assertTrue(PATH_BACKUP_FILE.toFile().exists());
        Assertions.assertTrue(PATH_UPDATE_FILE.toFile().exists());
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_FILE), "Agent_Update_File_Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_BACKUP_FILE), "Agent_File_Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_UPDATE_FILE), "Agent_Update_File_Content".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void startUpdateProcess_invalidInput() {
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.startUpdateProcess(null));
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.startUpdateProcess(""));
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.startUpdateProcess("   "));
    }

    @Test
    void startUpdateProcess_wrongInput() {
        Mockito.when(cryptoHandler.calculateChecksumOfFile(Mockito.any())).thenReturn("valid CheckSum");
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Files.writeString(PATH_UPDATE_FILE, "Agent-Update_Content");
            return true;
        });
        Assertions.assertThrows(SevereAgentErrorException.class, () -> updateHandler.startUpdateProcess("invalid CheckSum"));
        Assertions.assertFalse(PATH_UPDATE_FILE.toFile().exists());
    }

    @Test
    void startUpdateProcess_correctInput() throws IOException {
        Mockito.when(cryptoHandler.calculateChecksumOfFile(Mockito.any())).thenReturn("valid CheckSum");
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Files.writeString(PATH_UPDATE_FILE, "Agent-Update_Content");
            return true;
        });
        Assertions.assertDoesNotThrow(() -> updateHandler.startUpdateProcess("valid CheckSum"));
        Assertions.assertTrue(PATH_UPDATE_FILE.toFile().exists());
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_UPDATE_FILE), "Agent-Update_Content".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void startUpdateProcess_correctInputOldFilePresent() throws IOException {
        Files.writeString(PATH_UPDATE_FILE, "Old-Agent-Update_Content");
        Mockito.when(cryptoHandler.calculateChecksumOfFile(Mockito.any())).thenReturn("valid CheckSum");
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Files.writeString(PATH_UPDATE_FILE, "Agent-Update_Content");
            return true;
        });
        Assertions.assertDoesNotThrow(() -> updateHandler.startUpdateProcess("valid CheckSum"));
        Assertions.assertTrue(PATH_UPDATE_FILE.toFile().exists());
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_UPDATE_FILE), "Agent-Update_Content".getBytes(StandardCharsets.UTF_8));
    }

}
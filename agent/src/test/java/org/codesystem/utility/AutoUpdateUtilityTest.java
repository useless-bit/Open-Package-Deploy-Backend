package org.codesystem.utility;

import org.codesystem.exceptions.SevereAgentErrorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class AutoUpdateUtilityTest {

    String FILE_NAME_AGENT_UPDATE = "Agent_update.jar";
    Path PATH_UPDATE_FILE = Paths.get(FILE_NAME_AGENT_UPDATE);
    String FILE_NAME_AGENT_UPDATE_BACKUP = "Agent_backup.jar";
    Path PATH_BACKUP_FILE = Paths.get(FILE_NAME_AGENT_UPDATE_BACKUP);
    String FILE_NAME_AGENT = "Agent.jar";
    Path PATH_FILE = Paths.get(FILE_NAME_AGENT);
    AutoUpdateUtility autoUpdateUtility;
    MockedStatic<SystemExitUtility> systemExitMockedStatic;

    @BeforeEach
    void setUp() {
        autoUpdateUtility = new AutoUpdateUtility();

        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).then(invocationOnMock -> null);
        deleteFiles();
    }

    @AfterEach
    void tearDown() {
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
        Assertions.assertThrows(SevereAgentErrorException.class, () -> autoUpdateUtility.updateApplication());
    }

    @Test
    void updateApplication_agentPresentUpdateMissing() throws IOException {
        Files.writeString(PATH_FILE, "Agent_File_Content");
        Assertions.assertThrows(SevereAgentErrorException.class, () -> autoUpdateUtility.updateApplication());
    }

    @Test
    void updateApplication_agentMissingUpdatePresent() throws IOException {
        Files.writeString(PATH_UPDATE_FILE, "Agent_Update_File_Content");
        Assertions.assertThrows(SevereAgentErrorException.class, () -> autoUpdateUtility.updateApplication());
    }

    @Test
    void updateApplication_agentPresentUpdatePresent() throws IOException {
        Files.writeString(PATH_FILE, "Agent_File_Content");
        Files.writeString(PATH_UPDATE_FILE, "Agent_Update_File_Content");
        Assertions.assertDoesNotThrow(() -> autoUpdateUtility.updateApplication());
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
        Assertions.assertDoesNotThrow(() -> autoUpdateUtility.updateApplication());
        Assertions.assertTrue(PATH_FILE.toFile().exists());
        Assertions.assertTrue(PATH_BACKUP_FILE.toFile().exists());
        Assertions.assertTrue(PATH_UPDATE_FILE.toFile().exists());
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_FILE), "Agent_Update_File_Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_BACKUP_FILE), "Agent_File_Content".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(Files.readAllBytes(PATH_UPDATE_FILE), "Agent_Update_File_Content".getBytes(StandardCharsets.UTF_8));
    }
}
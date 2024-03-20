package org.codesystem.utility;

import org.codesystem.exceptions.SevereAgentErrorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AutoUpdateUtility {
    String FILE_NAME_AGENT_UPDATE = "Agent_update.jar";
    Path PATH_UPDATE_FILE = Paths.get(FILE_NAME_AGENT_UPDATE);
    String FILE_NAME_AGENT_UPDATE_BACKUP = "Agent_backup.jar";
    Path PATH_BACKUP_FILE = Paths.get(FILE_NAME_AGENT_UPDATE_BACKUP);
    String FILE_NAME_AGENT = "Agent.jar";
    Path PATH_FILE = Paths.get(FILE_NAME_AGENT);

    public void updateApplication() {
        if (!PATH_UPDATE_FILE.toFile().exists() || !PATH_FILE.toFile().exists()) {
            throw new SevereAgentErrorException("Cannot find update file");
        }

        if (PATH_BACKUP_FILE.toFile().exists()) {
            try {
                Files.delete(PATH_BACKUP_FILE);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old backup: " + e.getMessage());
            }
        }

        try {
            Files.copy(PATH_FILE, PATH_BACKUP_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot create backup: " + e.getMessage());
        }
        try {
            Files.delete(PATH_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot delete old Agent: " + e.getMessage());
        }

        try {
            Files.copy(PATH_UPDATE_FILE, PATH_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot copy new Agent: " + e.getMessage());
        }
        SystemExitUtility.exit(0);
    }

}

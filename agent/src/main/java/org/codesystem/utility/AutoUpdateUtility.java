package org.codesystem.utility;

import org.codesystem.Variables;
import org.codesystem.exceptions.SevereAgentErrorException;

import java.io.IOException;
import java.nio.file.Files;

public class AutoUpdateUtility {

    public void updateApplication() {
        if (!Variables.PATH_UPDATE_FILE.toFile().exists() || !Variables.PATH_FILE.toFile().exists()) {
            throw new SevereAgentErrorException("Cannot find update file");
        }

        if (Variables.PATH_BACKUP_FILE.toFile().exists()) {
            try {
                Files.delete(Variables.PATH_BACKUP_FILE);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete old backup: " + e.getMessage());
            }
        }

        try {
            Files.copy(Variables.PATH_FILE, Variables.PATH_BACKUP_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot create backup: " + e.getMessage());
        }
        try {
            Files.delete(Variables.PATH_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot delete old Agent: " + e.getMessage());
        }

        try {
            Files.copy(Variables.PATH_UPDATE_FILE, Variables.PATH_FILE);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot copy new Agent: " + e.getMessage());
        }
        SystemExitUtility.exit(0);
    }

}

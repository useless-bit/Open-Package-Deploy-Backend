package org.codesystem;

import okhttp3.MediaType;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Variables {
    // files
    public static final String FILE_NAME_AGENT_UPDATE = "Agent_update.jar";
    public static final String FILE_NAME_AGENT_UPDATE_BACKUP = "Agent_backup.jar";
    public static final String FILE_NAME_AGENT = "Agent.jar";
    public static final Path PATH_UPDATE_FILE = Paths.get(FILE_NAME_AGENT_UPDATE);
    public static final Path PATH_BACKUP_FILE = Paths.get(FILE_NAME_AGENT_UPDATE_BACKUP);
    public static final Path PATH_FILE = Paths.get(FILE_NAME_AGENT);
    public static final String FILE_NAME_PACKAGE_ENCRYPTED = "download"+ File.separator+"file";
    public static final String FILE_NAME_PACKAGE_DECRYPTED = "download"+File.separator+"file.zip";
    public static final Path PATH_PACKAGE_ENCRYPTED = Paths.get(FILE_NAME_PACKAGE_ENCRYPTED);

    //urls
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    public static final String URL_AGENT_DOWNLOAD = "/api/agent/communication/agent";
    public static final String URL_UPDATE_CHECK_REQUEST = "/api/agent/communication/checkForUpdates";
    public static final String URL_PACKAGE_DETAIL = "/api/agent/communication/package";
    public static final String URL_DEPLOYMENT_RESULT = "/api/agent/communication/deploymentResult";
    public static final String URL_REGISTRATION_REQUEST = "/api/agent/registration";
    public static final String URL_REGISTRATION_VERIFICATION_REQUEST = "/api/agent/registration/verify";

    // properties
    public static final String PROPERTIES_SERVER_REGISTRATION_TOKEN = "Server.Registration-Token";
    public static final String PROPERTIES_SERVER_URL = "Server.Url";
    public static final String PROPERTIES_SERVER_REGISTERED = "Server.Registered";
    public static final String PROPERTIES_SERVER_ECC_PUBLIC_KEY = "Server.ECC.Public-Key";
    public static final String PROPERTIES_AGENT_ECC_PUBLIC_KEY = "Agent.ECC.Public-Key";
    public static final String PROPERTIES_AGENT_ECC_PRIVATE_KEY = "Agent.ECC.Private-Key";
    public static final String PROPERTIES_AGENT_UPDATE_INTERVAL = "Agent.Update-Interval";

    private Variables() {
    }
}

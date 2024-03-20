package org.codesystem;

public class Variables {
    // files
    public static final String FILE_NAME_AGENT_UPDATE = "Agent_update.jar";

    //urls
    public static final String URL_AGENT_DOWNLOAD = "/api/agent/communication/agent";
    public static final String URL_UPDATE_CHECK_REQUEST = "/api/agent/communication/checkForUpdates";

    // properties
    public static final String PROPERTIES_SERVER_ECC_PUBLIC_KEY = "Server.ECC.Public-Key";
    public static final String PROPERTIES_AGENT_ECC_PUBLIC_KEY = "Agent.ECC.Public-Key";
    public static final String PROPERTIES_AGENT_ECC_PRIVATE_KEY = "Agent.ECC.Private-Key";
    public static final String PROPERTIES_SERVER_REGISTRATION_TOKEN = "Server.Registration-Token";

    private Variables() {
    }
}

package org.codesystem.server;

public class Variables {
    public static final String CONTENT_TYPE_ZIP_FILE = "application/zip";

    // Requests
    public static final String JSON_FIELD_SIGNATURE = "signature";
    public static final String JSON_FIELD_TIMESTAMP = "timestamp";

    // Response Entity text
    public static final String ERROR_RESPONSE_INVALID_REQUEST = "Invalid Request";
    public static final String ERROR_RESPONSE_INVALID_KEY = "Invalid Key";
    public static final String ERROR_RESPONSE_INVALID_DEPLOYMENT = "Invalid Deployment";
    public static final String ERROR_RESPONSE_NO_DEPLOYMENT_AVAILABLE = "No Deployment available";
    public static final String ERROR_RESPONSE_NO_DEPLOYMENT = "Deployment not found";
    public static final String ERROR_RESPONSE_NO_GROUP = "Group not found";
    public static final String ERROR_RESPONSE_NO_AGENT = "Agent not found";
    public static final String ERROR_RESPONSE_NO_PACKAGE = "Package not found";
    public static final String ERROR_RESPONSE_INVALID_ZIP_FILE = "Invalid zip-file";
    public static final String ERROR_RESPONSE_INVALID_INTERVAL = "Invalid interval";
    public static final String ERROR_RESPONSE_CANNOT_STORE_FILE = "Error when storing file";
    public static final String ERROR_RESPONSE_AGENT_REGISTRATION_CANNOT_VERIFY = "Cannot verify Agent";
    public static final String ERROR_RESPONSE_AGENT_REGISTRATION_ALREADY_REGISTERED = "A Agent with this public key is already registered";

    // Package
    public static final String PACKAGE_AGENT_ERROR_BEGINNING = "AGENT-DEPLOYMENT-ERROR";
    public static final String PACKAGE_ERROR_ZIP_FILE_CHECKSUM = "Error when reading zip-file and creating checksum";
    public static final String PACKAGE_ERROR_CHECKSUM_MISMATCH = "Checksum mismatch";
    public static final String PACKAGE_ERROR_CANNOT_DELETE_PACKAGE_PROCESSING = "Cannot delete package during processing";
    public static final String PACKAGE_ERROR_ALREADY_DELETED = "Package already marked for deletion";

    // Agent
    public static final String AGENT_ERROR_INVALID_OS = "Cannot set 'UNKNOWN' OperatingSystem";
    public static final String AGENT_ERROR_CHANGING_OS = "Cannot change Operating System";

    // Deployment
    public static final String DEPLOYMENT_ERROR_PACKAGE_NOT_AVAILABLE = "Package not available for deployment";
    public static final String DEPLOYMENT_ERROR_ALREADY_PRESENT = "Deployment already present";
    public static final String DEPLOYMENT_ERROR_INVALID_OS = "Invalid OS";
    public static final String DEPLOYMENT_ERROR_OS_MISMATCH = "OS mismatch";


    private Variables() {
    }
}

package org.codesystem.server.enums.packages;

public enum PackageStatusInternal {
    PROCESSED,
    PROCESSING,
    UPLOADED,
    MARKED_AS_DELETED,
    ERROR,
    ERROR_FILE_NOT_FOUND,
    ERROR_CHECKSUM_MISMATCH,
    ERROR_ENCRYPTION,
    ERROR_DECRYPTION
}

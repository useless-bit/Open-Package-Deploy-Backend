package org.codesystem;

public final class SystemExit {
    public SystemExit() {
    }

    public static void exit(int returnValue) {
        System.exit(returnValue);
    }
}

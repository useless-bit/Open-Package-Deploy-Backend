package org.codesystem;

public class SystemExit {
    private SystemExit() {
    }

    public static void exit(int returnValue) {
        System.exit(returnValue);
    }
}

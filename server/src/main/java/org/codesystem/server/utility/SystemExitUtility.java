package org.codesystem.server.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemExitUtility {
    public void exit(int exitCode) {
        System.exit(exitCode);
    }
}

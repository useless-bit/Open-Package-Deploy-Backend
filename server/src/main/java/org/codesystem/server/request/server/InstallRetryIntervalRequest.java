package org.codesystem.server.request.server;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstallRetryIntervalRequest {
    private Integer installRetryInterval;
}

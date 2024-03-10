package org.codesystem.server.request.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstallRetryIntervalRequest {
    private Integer installRetryInterval;
}

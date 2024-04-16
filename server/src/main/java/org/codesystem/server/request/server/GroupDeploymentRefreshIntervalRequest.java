package org.codesystem.server.request.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDeploymentRefreshIntervalRequest {
    private Integer groupDeploymentRefreshInterval;
}

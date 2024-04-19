package org.codesystem.server.request.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codesystem.server.enums.agent.OperatingSystem;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateEmptyGroupRequest {
    private String name;
    private String description;
    private OperatingSystem operatingSystem;
}

package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codesystem.server.enums.agent.OperatingSystem;

@Getter
@AllArgsConstructor
public class AddNewPackageRequest {
    private String packageName;
    private String packageChecksum;
    private OperatingSystem operatingSystem;
    private String expectedReturnValue;
}

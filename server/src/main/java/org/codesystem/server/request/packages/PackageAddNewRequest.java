package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codesystem.server.enums.agent.OperatingSystem;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PackageAddNewRequest {
    private String packageName;
    private String packageChecksum;
    private OperatingSystem operatingSystem;
    private String expectedReturnValue;
}

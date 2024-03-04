package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codesystem.server.enums.agent.OperatingSystem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageRequest {
    private String packageName;
}

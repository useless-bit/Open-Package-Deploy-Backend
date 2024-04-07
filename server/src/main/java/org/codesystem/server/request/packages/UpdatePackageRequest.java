package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdatePackageRequest {
    private String packageName;
    private String expectedReturnValue;
}

package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePackageRequest {
    private String packageName;
    private String expectedReturnValue;
}

package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdatePackageContentRequest {
    private String packageChecksum;
}

package org.codesystem.server.request.packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PackageUpdateContentRequest {
    private String packageChecksum;
}

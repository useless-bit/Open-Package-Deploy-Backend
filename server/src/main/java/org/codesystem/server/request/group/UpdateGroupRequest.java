package org.codesystem.server.request.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGroupRequest {
    private String name;
    private String description;
}

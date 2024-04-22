package org.codesystem.server.response.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codesystem.server.response.general.ApiResponse;

@Getter
@Setter
@AllArgsConstructor
public class GroupCreateResponse implements ApiResponse {
    private String groupUUID;
}

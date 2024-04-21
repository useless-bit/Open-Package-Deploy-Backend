package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.group.CreateEmptyGroupRequest;
import org.codesystem.server.request.group.UpdateGroupRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.group.ManagementGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Management Group")
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class ManagementGroupController {
    private final ManagementGroupService managementGroupService;

    @GetMapping()
    public ResponseEntity<ApiResponse> getAllGroups() {
        return managementGroupService.getAllGroups();
    }

    @GetMapping("{groupUUID}")
    public ResponseEntity<ApiResponse> getGroup(@PathVariable String groupUUID) {
        return managementGroupService.getGroup(groupUUID);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse> getGroup(@RequestBody CreateEmptyGroupRequest createEmptyGroupRequest) {
        return managementGroupService.createEmptyGroup(createEmptyGroupRequest);
    }

    @DeleteMapping("{groupUUID}")
    public ResponseEntity<ApiResponse> deleteGroup(@PathVariable String groupUUID) {
        return managementGroupService.deleteGroup(groupUUID);
    }

    @PatchMapping("{groupUUID}")
    public ResponseEntity<ApiResponse> updateGroup(@RequestBody UpdateGroupRequest updateGroupRequest, @PathVariable String groupUUID) {
        return managementGroupService.updateGroup(groupUUID, updateGroupRequest);
    }

    @PostMapping("{groupUUID}/member/{agentUUID}")
    public ResponseEntity<ApiResponse> addAgent(@PathVariable String groupUUID, @PathVariable String agentUUID) {
        return managementGroupService.addAgent(groupUUID, agentUUID);
    }

    @DeleteMapping("{groupUUID}/member/{agentUUID}")
    public ResponseEntity<ApiResponse> removeAgent(@PathVariable String groupUUID, @PathVariable String agentUUID) {
        return managementGroupService.removeAgent(groupUUID, agentUUID);
    }

    @DeleteMapping("{groupUUID}/package/{packageUUID}")
    public ResponseEntity<ApiResponse> removePackage(@PathVariable String groupUUID, @PathVariable String packageUUID) {
        return managementGroupService.removePackage(groupUUID, packageUUID);
    }

    @PostMapping("{groupUUID}/package/{packageUUID}")
    public ResponseEntity<ApiResponse> addPackage(@PathVariable String groupUUID, @PathVariable String packageUUID) {
        return managementGroupService.addPackage(groupUUID, packageUUID);
    }

    @GetMapping("{groupUUID}/member")
    public ResponseEntity<ApiResponse> getMembers(@PathVariable String groupUUID) {
        return managementGroupService.getMembers(groupUUID);
    }

    @GetMapping("{groupUUID}/package")
    public ResponseEntity<ApiResponse> getPackages(@PathVariable String groupUUID) {
        return managementGroupService.getPackages(groupUUID);
    }
}

package org.codesystem.server.service.group;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.GroupRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.group.CreateEmptyGroupRequest;
import org.codesystem.server.request.group.UpdateGroupRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.group.CreateGroupResponse;
import org.codesystem.server.response.group.GetAllGroupsResponse;
import org.codesystem.server.response.group.GetGroupResponse;
import org.codesystem.server.service.server.LogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagementGroupService {
    private final GroupRepository groupRepository;
    private final AgentRepository agentRepository;
    private final PackageRepository packageRepository;
    private final LogService logService;


    public ResponseEntity<ApiResponse> getAllGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(new GetAllGroupsResponse(groupRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getGroup(String groupUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new GetGroupResponse(groupEntity));
    }

    public ResponseEntity<ApiResponse> createEmptyGroup(CreateEmptyGroupRequest createEmptyGroupRequest) {
        if (createEmptyGroupRequest == null || createEmptyGroupRequest.getName() == null || createEmptyGroupRequest.getName().isBlank()
                || createEmptyGroupRequest.getOperatingSystem() == null || createEmptyGroupRequest.getOperatingSystem() == OperatingSystem.UNKNOWN) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(createEmptyGroupRequest.getName().trim());
        if (createEmptyGroupRequest.getDescription() != null && !createEmptyGroupRequest.getDescription().isBlank()) {
            groupEntity.setDescription(createEmptyGroupRequest.getDescription().trim());
        }
        groupEntity.setOperatingSystem(createEmptyGroupRequest.getOperatingSystem());
        groupEntity = groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).body(new CreateGroupResponse(groupEntity.getUuid()));
    }


    public ResponseEntity<ApiResponse> updateGroup(String groupUUID, UpdateGroupRequest updateGroupRequest) {
        if (updateGroupRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        if (updateGroupRequest.getName() != null && !updateGroupRequest.getName().isBlank()) {
            groupEntity.setName(updateGroupRequest.getName().trim());
        }
        if (updateGroupRequest.getDescription() != null && !updateGroupRequest.getDescription().isBlank()) {
            groupEntity.setDescription(updateGroupRequest.getDescription().trim());
        }
        groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> addAgent(String groupUUID, String agentUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        if (agentEntity.getOperatingSystem() != groupEntity.getOperatingSystem()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_OS_MISMATCH));
        }
        groupEntity.addMember(agentEntity);
        groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> removeAgent(String groupUUID, String agentUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        AgentEntity agentEntity = agentRepository.findFirstByUuid(agentUUID);
        if (agentEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_AGENT));
        }
        groupEntity.removeMember(agentEntity);
        groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> addPackage(String groupUUID, String packageUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        if (packageEntity.getTargetOperatingSystem() != groupEntity.getOperatingSystem()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_OS_MISMATCH));
        }
        groupEntity.addPackage(packageEntity);
        groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity<ApiResponse> removePackage(String groupUUID, String packageUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        groupEntity.removePackage(packageEntity);
        groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

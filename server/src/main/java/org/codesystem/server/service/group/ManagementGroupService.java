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
import org.codesystem.server.request.group.GroupCreateEmptyRequest;
import org.codesystem.server.request.group.GroupUpdateRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.group.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagementGroupService {
    private final GroupRepository groupRepository;
    private final AgentRepository agentRepository;
    private final PackageRepository packageRepository;

    public ResponseEntity<ApiResponse> getAllGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(new GroupInfoListResponse(groupRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getGroup(String groupUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new GroupInfoResponse(groupEntity));
    }

    public ResponseEntity<ApiResponse> createEmptyGroup(GroupCreateEmptyRequest groupCreateEmptyRequest) {
        if (groupCreateEmptyRequest == null || groupCreateEmptyRequest.getName() == null || groupCreateEmptyRequest.getName().isBlank()
                || groupCreateEmptyRequest.getOperatingSystem() == null || groupCreateEmptyRequest.getOperatingSystem() == OperatingSystem.UNKNOWN) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupCreateEmptyRequest.getName().trim());
        if (groupCreateEmptyRequest.getDescription() != null && !groupCreateEmptyRequest.getDescription().isBlank()) {
            groupEntity.setDescription(groupCreateEmptyRequest.getDescription().trim());
        }
        groupEntity.setOperatingSystem(groupCreateEmptyRequest.getOperatingSystem());
        groupEntity = groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).body(new GroupCreateResponse(groupEntity.getUuid()));
    }

    public ResponseEntity<ApiResponse> deleteGroup(String groupUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        groupRepository.delete(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();

    }

    public ResponseEntity<ApiResponse> updateGroup(String groupUUID, GroupUpdateRequest groupUpdateRequest) {
        if (groupUpdateRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        if (groupUpdateRequest.getName() != null && !groupUpdateRequest.getName().isBlank()) {
            groupEntity.setName(groupUpdateRequest.getName().trim());
        }
        if (groupUpdateRequest.getDescription() != null && !groupUpdateRequest.getDescription().isBlank()) {
            groupEntity.setDescription(groupUpdateRequest.getDescription().trim());
        } else {
            groupEntity.setDescription(null);
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

    public ResponseEntity<ApiResponse> getMembers(String groupUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new GroupMemberListResponse(groupEntity.getMembers()));
    }

    public ResponseEntity<ApiResponse> getPackages(String groupUUID) {
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new GroupPackageListResponse(groupEntity.getDeployedPackages()));
    }
}

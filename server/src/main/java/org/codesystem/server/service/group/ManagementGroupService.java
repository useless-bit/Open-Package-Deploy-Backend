package org.codesystem.server.service.group;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.GroupRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.group.CreateEmptyGroupRequest;
import org.codesystem.server.request.group.UpdateGroupRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.group.CreateGroupResponse;
import org.codesystem.server.response.group.GetAllGroupsResponse;
import org.codesystem.server.response.group.GetGroupResponse;
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
        if (createEmptyGroupRequest == null || createEmptyGroupRequest.getName() == null || createEmptyGroupRequest.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        GroupEntity groupEntity = new GroupEntity(createEmptyGroupRequest.getName().trim(), createEmptyGroupRequest.getDescription().trim());
        groupEntity = groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).body(new CreateGroupResponse(groupEntity.getUuid()));
    }


    public ResponseEntity<ApiResponse> updateGroup(UpdateGroupRequest updateGroupRequest, String groupUUID) {
        if (updateGroupRequest == null || updateGroupRequest.getName() == null || updateGroupRequest.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        GroupEntity groupEntity = groupRepository.findFirstByUuid(groupUUID);
        if (groupEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        groupEntity.setName(updateGroupRequest.getName().trim());
        groupEntity.setDescription(updateGroupRequest.getDescription().trim());
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Variables.ERROR_RESPONSE_NO_GROUP));
        }
        groupEntity.addMember(agentEntity);
        groupRepository.save(groupEntity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

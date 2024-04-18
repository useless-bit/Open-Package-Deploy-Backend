package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.*;
import org.codesystem.server.service.server.LogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DeploymentValidator {
    private final ServerRepository serverRepository;
    private final DeploymentRepository deploymentRepository;
    private final AgentRepository agentRepository;
    private final PackageRepository packageRepository;
    private final GroupRepository groupRepository;
    private final LogService logService;

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 1)
    @Async("validateDeployments")
    public void validateDeployments() {
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        if (serverEntity.getLastDeploymentValidation() == null || Instant.now().isAfter(serverEntity.getLastDeploymentValidation().plus(serverEntity.getDeploymentValidationInterval(), ChronoUnit.SECONDS))) {
            List<AgentEntity> agentEntities = agentRepository.findAllRegistered();
            List<PackageEntity> packageEntities = packageRepository.findAll();

            for (AgentEntity agentEntity : agentEntities) {
                List<PackageEntity> filteredPackageEntities = packageEntities.stream().filter(entity -> entity.getTargetOperatingSystem().equals(agentEntity.getOperatingSystem())).toList();
                for (PackageEntity packageEntity : filteredPackageEntities) {
                    if (deploymentRepository.findAllByAgentUUIDAndPackageUUID(agentEntity.getUuid(), packageEntity.getUuid()).size() > 1) {
                        deleteDuplicateDeployments(agentEntity, packageEntity);
                    }
                    addMissingDeployment(agentEntity, packageEntity);
                    removeUnreferencedDeployments(agentEntity, packageEntity);
                }
            }
        }
    }


    private void deleteDuplicateDeployments(AgentEntity agentEntity, PackageEntity packageEntity) {
    }

    private void addMissingDeployment(AgentEntity agentEntity, PackageEntity packageEntity) {
    }

    private void removeUnreferencedDeployments(AgentEntity agentEntity, PackageEntity packageEntity) {
    }
}

package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.DeploymentEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.*;
import org.codesystem.server.service.server.LogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
            logService.addEntry(Severity.INFO, "Deployment Validation started");
            Instant startTimestamp = Instant.now();
            List<AgentEntity> agentEntities = agentRepository.findAllRegistered();
            List<PackageEntity> packageEntities = packageRepository.findAll();

            long deletedDuplicateDeployments = 0;
            long createdGroupDeployments = 0;
            long deletedUnreferencedDeployments = 0;

            for (AgentEntity agentEntity : agentEntities) {
                List<PackageEntity> filteredPackageEntities = packageEntities.stream().filter(entity -> entity.getTargetOperatingSystem().equals(agentEntity.getOperatingSystem())).toList();
                for (PackageEntity packageEntity : filteredPackageEntities) {
                    if (deploymentRepository.findAllByAgentUUIDAndPackageUUID(agentEntity.getUuid(), packageEntity.getUuid()).size() > 1) {
                        deletedDuplicateDeployments += deleteDuplicateDeployments(agentEntity, packageEntity);
                    }
                    if (addMissingDeployment(agentEntity, packageEntity)) {
                        createdGroupDeployments += 1;
                    }
                    if (removeUnreferencedDeployments(agentEntity, packageEntity)) {
                        deletedUnreferencedDeployments += 1;
                    }
                }
            }
            serverEntity.setLastDeploymentValidation(Instant.now());
            serverRepository.save(serverEntity);
            logService.addEntry(Severity.INFO, "Deployment validation finished in: " +
                    Duration.between(startTimestamp, Instant.now()).getSeconds() +
                    ". Deleted Duplicate Deployments: " +
                    deletedDuplicateDeployments +
                    ". Created Group Deployments: " +
                    createdGroupDeployments +
                    ". Deleted Unreferenced Deployments: " +
                    deletedUnreferencedDeployments
            );
        }
    }

    /**
     * Deletes duplicate deployments.
     *
     * @return Returns the amount of duplicate/deleted deployments.
     */
    private int deleteDuplicateDeployments(AgentEntity agentEntity, PackageEntity packageEntity) {
        boolean isOneDeploymentDirect = false;
        List<DeploymentEntity> deploymentEntities = deploymentRepository.findAllByAgentUUIDAndPackageUUID(agentEntity.getUuid(), packageEntity.getUuid());
        for (DeploymentEntity deploymentEntity : deploymentEntities) {
            if (deploymentEntity.isDirectDeployment()) {
                isOneDeploymentDirect = true;
                break;
            }
        }
        deploymentRepository.deleteAll(deploymentEntities);
        DeploymentEntity deploymentEntity = new DeploymentEntity();
        deploymentEntity.setPackageEntity(packageEntity);
        deploymentEntity.setAgentEntity(agentEntity);
        deploymentEntity.setDirectDeployment(isOneDeploymentDirect);
        deploymentRepository.save(deploymentEntity);
        return deploymentEntities.size() - 1;
    }

    /**
     * Creates a missing group deployment.
     *
     * @return Returns true if the deployment got created.
     */
    private boolean addMissingDeployment(AgentEntity agentEntity, PackageEntity packageEntity) {
        if (!deploymentRepository.isDeploymentAlreadyPresent(agentEntity.getUuid(), packageEntity.getUuid()) && groupRepository.isPackageAvailableThroughGroup(agentEntity.getUuid(), packageEntity.getUuid())) {
            DeploymentEntity deploymentEntity = new DeploymentEntity();
            deploymentEntity.setPackageEntity(packageEntity);
            deploymentEntity.setAgentEntity(agentEntity);
            deploymentEntity.setDirectDeployment(false);
            deploymentRepository.save(deploymentEntity);
            return true;
        }
        return false;
    }

    /**
     * Deletes unreferenced group deployments.
     *
     * @return Returns true if the deployment for the package got deleted.
     */
    private boolean removeUnreferencedDeployments(AgentEntity agentEntity, PackageEntity packageEntity) {
        List<DeploymentEntity> deploymentEntities = deploymentRepository.findAllByAgentUUIDAndPackageUUID(agentEntity.getUuid(), packageEntity.getUuid());
        if (deploymentEntities.isEmpty()) {
            return false;
        }
        DeploymentEntity deploymentEntity = deploymentEntities.get(0);
        if (!deploymentEntity.isDirectDeployment() && !groupRepository.isPackageAvailableThroughGroup(agentEntity.getUuid(), packageEntity.getUuid())) {
            deploymentRepository.delete(deploymentEntity);
            return true;
        }
        return false;
    }
}

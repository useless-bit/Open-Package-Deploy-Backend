package org.codesystem.server.repository;

import org.codesystem.server.entity.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, String> {
    DeploymentEntity findFirstByUuid(String uuid);

    @Query("""
            select d from DeploymentEntity d
            where d.agentEntity.uuid = ?1 and d.deployed = false and d.lastDeploymentTimestamp < ?2""")
    List<DeploymentEntity> findAvailableDeployments(String uuid, Instant lastDeploymentTimestamp);

    @Query("""
            select d from DeploymentEntity d
            where d.agentEntity.uuid = ?1 and d.deployed = false and (d.lastDeploymentTimestamp < ?2 or d.lastDeploymentTimestamp is null)""")
    List<DeploymentEntity> findAvailableDeployments_test(String uuid, Instant lastDeploymentTimestamp);


    @Query("select (count(d) > 0) from DeploymentEntity d where d.agentEntity.uuid = ?1 and d.packageEntity.uuid = ?2")
    boolean isDeploymentAlreadyPresent(String agentUUID, String packageUUID);

    @Query("select d from DeploymentEntity d where d.agentEntity.uuid = ?1")
    List<DeploymentEntity> findDeploymentsForAgent(String agentUUID);


}

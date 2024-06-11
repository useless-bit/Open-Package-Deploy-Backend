package org.codesystem.server.repository;

import org.codesystem.server.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<AgentEntity, String> {
    AgentEntity findFirstByPublicKeyBase64(String publicKeyBase64);

    AgentEntity findFirstByUuid(String uuid);

    @Query("select a from AgentEntity a where a.registrationCompleted = true")
    List<AgentEntity> findAllRegistered();


}

package org.codesystem.server.repository;

import org.codesystem.server.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    GroupEntity findFirstByUuid(String uuid);
}

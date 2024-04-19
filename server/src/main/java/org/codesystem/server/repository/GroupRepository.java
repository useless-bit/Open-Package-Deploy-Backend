package org.codesystem.server.repository;

import org.codesystem.server.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    GroupEntity findFirstByUuid(String uuid);

    @Query("""
            select (count(g) > 0) from GroupEntity g inner join g.members members inner join g.deployedPackages deployedPackages
            where members.uuid = ?1 and deployedPackages.uuid = ?2""")
    boolean isPackageAvailableThroughGroup(String agentUUID, String packageUUID);


}

package org.codesystem.server.repository;

import org.codesystem.server.entity.SystemUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface SystemUsageRepository extends JpaRepository<SystemUsageEntity, String> {
    @Query("select l from SystemUsageEntity l order by l.timestamp DESC")
    List<SystemUsageEntity> findAllSorted();

    @Transactional
    @Modifying
    @Query("delete from SystemUsageEntity l where l.timestamp < ?1")
    int deleteAllOlderThan(Instant timestamp);
}

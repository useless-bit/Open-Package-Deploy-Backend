package org.codesystem.server.repository;

import org.codesystem.server.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntity, String> {
    @Query("select l from LogEntity l order by l.timestamp DESC")
    List<LogEntity> findAllSorted();

    @Transactional
    @Modifying
    @Query("delete from LogEntity l where l.timestamp < ?1")
    int deleteAllOlderThan(Instant timestamp);
}

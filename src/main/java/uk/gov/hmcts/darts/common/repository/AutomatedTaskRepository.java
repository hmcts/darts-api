package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutomatedTaskRepository extends RevisionRepository<AutomatedTaskEntity, Integer, Long>, JpaRepository<AutomatedTaskEntity, Integer> {
    Optional<AutomatedTaskEntity> findByTaskName(String taskName);

    @Query(value = """
        SELECT sl.lock_until
        FROM darts.shedlock sl
        WHERE sl.name = :taskName
        """, nativeQuery = true
    )
    List<Timestamp> findLockedUntilForTask(String taskName);


}
package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

@Repository
public interface AutomatedTaskRepository extends JpaRepository<AutomatedTaskEntity, Integer> {
    AutomatedTaskEntity findByTaskName(String name);
}

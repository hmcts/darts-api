package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

import java.util.List;

@Repository
public interface AutomatedTaskRepository extends JpaRepository<AutomatedTaskEntity, Integer> {

    AutomatedTaskEntity findByTaskName(String taskName);

    List<AutomatedTaskEntity> findAllByTaskName(String taskName);

}

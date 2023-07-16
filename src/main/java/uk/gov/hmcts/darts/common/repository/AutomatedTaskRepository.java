package uk.gov.hmcts.darts.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

import static uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity.TABLE_NAME;
import static uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity.TASK_NAME;


@Repository
public interface AutomatedTaskRepository extends JpaRepository<AutomatedTaskEntity, Integer> {

//    @Query(value = "SELECT * FROM {h-schema}" + TABLE_NAME + " aut " +
//        "WHERE aut." + TASK_NAME + " = :taskName "
//    )
    AutomatedTaskEntity getByTaskName(String taskName);

    AutomatedTaskEntity findByTaskName(String taskName);

    List<AutomatedTaskEntity> findAllByTaskName(String taskName);

}

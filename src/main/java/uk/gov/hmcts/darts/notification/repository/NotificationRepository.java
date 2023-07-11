package uk.gov.hmcts.darts.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;

import java.util.List;

@Repository
@SuppressWarnings("PMD.MethodNamingConventions")
public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {
    List<NotificationEntity> findByCourtCase_Id(Integer caseId);

    List<NotificationEntity> findByStatusIn(List<String> status);

}

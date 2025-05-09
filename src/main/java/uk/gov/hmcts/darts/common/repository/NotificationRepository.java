package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;

import java.util.List;

@Repository
@SuppressWarnings("PMD.MethodNamingConventions")
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByCourtCase_Id(Integer caseId);

    @Query("SELECT n.id from NotificationEntity n WHERE n.status in ?1")
    List<Long> findIdsByStatusIn(List<NotificationStatus> status);

}

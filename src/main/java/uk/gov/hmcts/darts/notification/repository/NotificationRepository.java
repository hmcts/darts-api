package uk.gov.hmcts.darts.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.notification.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends RevisionRepository<Notification, Integer, Long>, JpaRepository<Notification, Integer> {
    List<Notification> findByCaseId(String caseId);

    List<Notification> findByStatusIn(List<String> status);

}

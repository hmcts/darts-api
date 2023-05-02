package uk.gov.hmcts.darts.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.notification.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

}

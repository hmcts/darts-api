package uk.gov.hmcts.darts.log.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class NotificationLoggerServiceImpl implements NotificationLoggerService {
    @Override
    public void scheduleNotification(NotificationEntity notificationEntity, Integer caseId) {
        log.info("Notification scheduled: notificationId={}, type={}, caseId={}, status={}",
                 notificationEntity.getId(), notificationEntity.getEventId(), caseId,
                 notificationEntity.getStatus());
    }

    @Override
    public void sendingNotification(NotificationEntity notification, String templateId, Integer attempts) {
        log.info("Notification sending: notificationId={}, type={}, caseId={}, templateId={}, status={}, attemptNo={}",
                 notification.getId(), notification.getEventId(), notification.getCourtCase().getId(), templateId,
                 notification.getStatus(), notification.getAttempts());
    }

    @Override
    public void sentNotification(NotificationEntity notification, String templateId, Integer attempts) {
        log.info("Notification sent: notificationId={}, type={}, caseId={}, templateId={}, status={}, attemptNo={}",
                 notification.getId(), notification.getEventId(), notification.getCourtCase().getId(), templateId,
                 notification.getStatus(), notification.getAttempts());
    }

    @Override
    public void errorRetryingNotification(NotificationEntity notification, String templateId, NotificationClientException e) {
        log.info("Notification GovNotify error, retrying: notificationId={}, type={}, caseId={}, templateId={}, status={}, attemptNo={}, error={}",
                 notification.getId(), notification.getEventId(), notification.getCourtCase().getId(), templateId, notification.getStatus(),
                 notification.getAttempts(), e.getMessage());
    }

    @Override
    public void failedNotification(NotificationEntity notification, String templateId, NotificationClientException e) {
        log.info("Notification failed to send: notificationId={}, type={}, caseId={}, templateId={}, status={}, attemptNo={}, error={}",
                 notification.getId(), notification.getEventId(), notification.getCourtCase().getId(), templateId, notification.getStatus(),
                 notification.getAttempts(), e.getMessage());
    }
}

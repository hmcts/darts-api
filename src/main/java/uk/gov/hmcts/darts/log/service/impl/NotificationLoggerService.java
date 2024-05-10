package uk.gov.hmcts.darts.log.service.impl;

import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.service.notify.NotificationClientException;

public interface NotificationLoggerService {
    void scheduleNotification(NotificationEntity notificationEntity, Integer caseId);

    void sendingNotification(NotificationEntity notification, String templateId, Integer attempts);

    void sentNotification(NotificationEntity notification, String templateId, Integer attempts);

    void errorRetryingNotification(NotificationEntity notification, String templateId, NotificationClientException ex);

    void failedNotification(NotificationEntity notification, String templateId, NotificationClientException ex);
}

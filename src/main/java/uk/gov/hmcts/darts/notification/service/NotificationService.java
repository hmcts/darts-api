package uk.gov.hmcts.darts.notification.service;

import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

public interface NotificationService {
    void scheduleNotification(SaveNotificationToDbRequest request);

    void sendNotificationToGovNotify();

    void sendNotificationToGovNotifyNow();
}

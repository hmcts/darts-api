package uk.gov.hmcts.darts.notification.service;

import uk.gov.hmcts.darts.notification.dto.CreateNotificationReq;
import uk.gov.hmcts.darts.notification.entity.Notification;

public interface NotificationService {
    Notification sendNotification(CreateNotificationReq createNotificationReq);
}

package uk.gov.hmcts.darts.notification.api;

import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

public interface NotificationApi {

    String getNotificationTemplateIdByName(String templateName) throws TemplateNotFoundException;

    void scheduleNotification(SaveNotificationToDbRequest saveNotificationToDbRequest);

}

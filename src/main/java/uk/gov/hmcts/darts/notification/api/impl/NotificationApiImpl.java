package uk.gov.hmcts.darts.notification.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.notification.service.NotificationService;

@Service
@RequiredArgsConstructor
public class NotificationApiImpl implements NotificationApi {

    private final TemplateIdHelper templateIdHelper;
    private final NotificationService notificationService;

    @Override
    public String getNotificationTemplateIdByName(String templateName) throws TemplateNotFoundException {
        return templateIdHelper.findTemplateId(templateName);
    }

    @Override
    public void scheduleNotification(SaveNotificationToDbRequest saveNotificationToDbRequest) {
        notificationService.scheduleNotification(saveNotificationToDbRequest);
    }

}

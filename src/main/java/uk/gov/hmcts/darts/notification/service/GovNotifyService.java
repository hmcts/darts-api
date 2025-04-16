package uk.gov.hmcts.darts.notification.service;

import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@FunctionalInterface
public interface GovNotifyService {

    SendEmailResponse sendNotification(GovNotifyRequest request) throws NotificationClientException;
}

package uk.gov.hmcts.darts.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.service.GovNotifyService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@RequiredArgsConstructor
@Service
@Slf4j
public class GovNotifyServiceImpl implements GovNotifyService {

    @Value("${darts.notification.gov-notify.api-key}")
    private String apiKey;

    private NotificationClient client;

    @Override
    public SendEmailResponse sendNotification(GovNotifyRequest request) throws NotificationClientException {

        log.trace("Sending email with following settings = {}", request);
        initiateGovNotifyClient();
        SendEmailResponse emailResponse;
        try {
            emailResponse = client.sendEmail(
                request.getTemplateId(),
                request.getEmailAddress(),
                request.getParameterMap(),
                request.getReference()
            );
        } catch (NotificationClientException e) {
            log.error("Notification with Id {} failed to send. Error message from GovNotify:- {}", request.getReference(), e.getMessage());
            throw e;
        }
        log.debug("Email sent successfully, response received from goNotify = {}", emailResponse);
        return emailResponse;
    }

    private void initiateGovNotifyClient(){
        if(client==null){
            client = new NotificationClient(apiKey);
        }
    }



}

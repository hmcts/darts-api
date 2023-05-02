package uk.gov.hmcts.darts.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.entity.Notification;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.notification.mapper.GovNotifyRequestMapper;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;
import uk.gov.hmcts.darts.notification.service.GovNotifyService;
import uk.gov.hmcts.darts.notification.service.NotificationService;
import uk.gov.service.notify.NotificationClientException;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;


@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final GovNotifyService govNotifyService;
    private final TemplateIdHelper templateIdHelper;
    private static final List<String> STATUS_ELIGIBLE_TO_SEND = Arrays.asList(
        String.valueOf(NotificationStatus.OPEN),
        String.valueOf(NotificationStatus.PROCESSING)
    );

    @Value("${darts.notification.max_retry_attempts}")
    private int maxRetry;

    @Override
    public void scheduleNotification(SaveNotificationToDbRequest request) {
        String emailAddresses = request.getEmailAddresses();
        String[] emailAddressList = emailAddresses.split(",");
        for (String emailAddress : emailAddressList) {
            saveNotificationToDb(request.getEventId(), request.getCaseId(), StringUtils.trim(emailAddress), request.getTemplateValues());
        }
    }

    private Notification saveNotificationToDb(String eventId, String caseId, String emailAddress, String templateValues) {
        EmailValidator emailValidator = EmailValidator.getInstance();
        if (!emailValidator.isValid(emailAddress)) {
            log.warn("The supplied email address, {}, is not valid, and so has been ignored.", emailAddress);
            return null;
        }
        Notification dbNotification = new Notification();
        dbNotification.setEventId(eventId);
        dbNotification.setCaseId(caseId);
        dbNotification.setEmailAddress(emailAddress);
        dbNotification.setStatus(String.valueOf(NotificationStatus.OPEN));
        dbNotification.setAttempts(0);
        dbNotification.setTemplateValues(templateValues);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dbNotification.setCreatedDateTime(now);
        dbNotification.setLastUpdatedDateTime(now);

        return notificationRepo.saveAndFlush(dbNotification);
    }


    @Override
    public void sendNotificationToGovNotify() {

        List<Notification> notificationEntries = notificationRepo.findByStatusIn(STATUS_ELIGIBLE_TO_SEND);
        for (Notification notification : notificationEntries) {
            String templateId;
            try {
                templateId = templateIdHelper.findTemplateId(notification.getEventId());
            } catch (TemplateNotFoundException e) {
                markNotificationAsFailed(notification);
                break;
            }

            GovNotifyRequest govNotifyRequest = null;
            try {
                govNotifyRequest = GovNotifyRequestMapper.map(notification, templateId);
                govNotifyService.sendNotification(govNotifyRequest);
                markNotificationAsSent(notification);
            } catch (JsonProcessingException e) {
                markNotificationAsFailed(notification);
            } catch (NotificationClientException e) {
                log.error(
                    "GovNotify has responded back with an error while trying to send Notification Id {}. Request={}, error={}",
                    notification.getId(),
                    govNotifyRequest,
                    e.getMessage()
                );
                incrementNotificationFailureCount(notification);
            }
        }
    }

    private void markNotificationAsFailed(Notification notification) {
        notification.setStatus(String.valueOf(NotificationStatus.FAILED));
        notificationRepo.saveAndFlush(notification);
    }

    private void markNotificationAsSent(Notification notification) {
        notification.setStatus(String.valueOf(NotificationStatus.SENT));
        notificationRepo.saveAndFlush(notification);
    }

    private void incrementNotificationFailureCount(Notification notification) {
        int attempts = notification.getAttempts();
        if (attempts < maxRetry) {
            notification.setAttempts(++attempts);
            notification.setStatus(String.valueOf(NotificationStatus.PROCESSING));
        } else {
            markNotificationAsFailed(notification);
        }
        notificationRepo.saveAndFlush(notification);
    }

}

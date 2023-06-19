package uk.gov.hmcts.darts.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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

import java.util.Arrays;
import java.util.List;


@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final GovNotifyService govNotifyService;
    private final TemplateIdHelper templateIdHelper;
    private final EmailValidator emailValidator = EmailValidator.getInstance();
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
            saveNotificationToDb(
                request.getEventId(),
                request.getCaseId(),
                StringUtils.trim(emailAddress),
                request.getTemplateValues()
            );
        }
    }

    private Notification saveNotificationToDb(String eventId, String caseId, String emailAddress, String templateValues) {
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

        return notificationRepo.saveAndFlush(dbNotification);
    }


    @Override
    @SchedulerLock(name = "NotificationService_sendNotificationToGovNotify",
        lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
    @Scheduled(cron = "${darts.notification.scheduler.cron}")
    public void sendNotificationToGovNotify() {
        log.debug("sendNotificationToGovNotify scheduler started.");

        List<Notification> notificationEntries = notificationRepo.findByStatusIn(STATUS_ELIGIBLE_TO_SEND);
        int notificationCounter = 0;
        for (Notification notification : notificationEntries) {
            log.trace("Processing {} of {}, Id {}.", ++notificationCounter, notificationEntries.size(), notification.getId());
            String templateId;
            try {
                templateId = templateIdHelper.findTemplateId(notification.getEventId());
            } catch (TemplateNotFoundException e) {
                updateNotificationStatus(notification, NotificationStatus.FAILED);
                break;
            }

            GovNotifyRequest govNotifyRequest = null;
            try {
                govNotifyRequest = GovNotifyRequestMapper.map(notification, templateId);
                govNotifyService.sendNotification(govNotifyRequest);
                updateNotificationStatus(notification, NotificationStatus.SENT);
            } catch (JsonProcessingException e) {
                updateNotificationStatus(notification, NotificationStatus.FAILED);
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

    private void updateNotificationStatus(Notification notification, NotificationStatus status) {
        notification.setStatus(String.valueOf(status));
        notificationRepo.saveAndFlush(notification);
    }

    private void incrementNotificationFailureCount(Notification notification) {
        int attempts = notification.getAttempts();
        attempts++;
        if (attempts <= maxRetry) {
            notification.setAttempts(attempts);
            notification.setStatus(String.valueOf(NotificationStatus.PROCESSING));
            log.info("Notification has failed to send, retrying ID: {}", notification.getId());
        } else {
            updateNotificationStatus(notification, NotificationStatus.FAILED);
            log.error("Notification has fully failed");
        }
        notificationRepo.saveAndFlush(notification);
    }

}

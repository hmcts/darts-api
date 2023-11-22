package uk.gov.hmcts.darts.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.NotificationRepository;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.GovNotifyRequestHelper;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.notification.service.GovNotifyService;
import uk.gov.hmcts.darts.notification.service.NotificationService;
import uk.gov.service.notify.NotificationClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;

    private final CaseRepository caseRepository;
    private final GovNotifyService govNotifyService;
    private final TemplateIdHelper templateIdHelper;
    private final GovNotifyRequestHelper govNotifyRequestHelper;
    private final EmailValidator emailValidator = EmailValidator.getInstance();
    private static final List<NotificationStatus> STATUS_ELIGIBLE_TO_SEND = Arrays.asList(
        NotificationStatus.OPEN,
        NotificationStatus.PROCESSING
    );

    @Value("${darts.notification.max_retry_attempts}")
    private int maxRetry;

    @Override
    @Transactional
    public void scheduleNotification(SaveNotificationToDbRequest request) {
        List<String> emailAddresses = getEmailAddresses(request);
        String templateParamsString = getTemplateParamsString(request);
        for (String emailAddress : emailAddresses) {
            saveNotificationToDb(
                request.getEventId(),
                request.getCaseId(),
                StringUtils.trim(emailAddress),
                templateParamsString
            );
        }
    }

    private String getTemplateParamsString(SaveNotificationToDbRequest request) {

        Map<String, String> templateValues = request.getTemplateValues();

        if (MapUtils.isEmpty(templateValues)) {
            return null;
        }

        ObjectWriter objectWriter = new ObjectMapper().writerFor(HashMap.class);
        try {
            return objectWriter.writeValueAsString(templateValues);
        } catch (JsonProcessingException e) {
            log.error(
                "Serialisation of request params for event {} with params {} has failed with error :- {}",
                request.getEventId(), request.getTemplateValues(),
                e.getMessage()
            );
        }
        return null;
    }

    private List<String> getEmailAddresses(SaveNotificationToDbRequest request) {
        String emailAddressesStr = request.getEmailAddresses();
        List<String> emailAddressList = new ArrayList<>();
        if (StringUtils.isNotBlank(emailAddressesStr)) {
            CollectionUtils.addAll(emailAddressList, StringUtils.split(emailAddressesStr, ", "));
        }
        if (request.getUserAccountsToEmail() != null) {
            CollectionUtils.addAll(
                emailAddressList,
                request.getUserAccountsToEmail().stream()
                    .map(UserAccountEntity::getEmailAddress)
                    .filter(emailAddress -> EmailValidator.getInstance().isValid(emailAddress))
                    .toList()
            );
        }

        return emailAddressList;
    }

    private NotificationEntity saveNotificationToDb(String eventId, Integer caseId, String emailAddress, String templateValues) {
        if (!emailValidator.isValid(emailAddress)) {
            log.warn("The supplied email address, {}, is not valid, and so has been ignored.", emailAddress);
            return null;
        }
        NotificationEntity dbNotification = new NotificationEntity();
        dbNotification.setEventId(eventId);
        dbNotification.setCourtCase(caseRepository.getReferenceById(caseId));
        dbNotification.setEmailAddress(emailAddress);
        dbNotification.setStatus(NotificationStatus.OPEN);
        dbNotification.setAttempts(0);
        dbNotification.setTemplateValues(templateValues);

        return notificationRepo.save(dbNotification);
    }

    @Override
    @SchedulerLock(name = "NotificationService_sendNotificationToGovNotify",
        lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
    @Scheduled(cron = "${darts.notification.scheduler.cron}")
    public void sendNotificationToGovNotify() {
        sendNotificationToGovNotifyNow();
    }

    @Override
    public void sendNotificationToGovNotifyNow() {
        log.debug("sendNotificationToGovNotify scheduler started.");

        List<NotificationEntity> notificationEntries = notificationRepo.findByStatusIn(STATUS_ELIGIBLE_TO_SEND);
        int notificationCounter = 0;
        for (NotificationEntity notification : notificationEntries) {
            log.trace(
                "Processing {} of {}, Id {}.",
                ++notificationCounter,
                notificationEntries.size(),
                notification.getId()
            );
            String templateId;
            try {
                templateId = templateIdHelper.findTemplateId(notification.getEventId());
            } catch (TemplateNotFoundException e) {
                updateNotificationStatus(notification, NotificationStatus.FAILED);
                break;
            }

            GovNotifyRequest govNotifyRequest = null;
            try {
                govNotifyRequest = govNotifyRequestHelper.map(notification, templateId);
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

    private void updateNotificationStatus(NotificationEntity notification, NotificationStatus status) {
        notification.setStatus(status);
        notificationRepo.saveAndFlush(notification);
    }

    private void incrementNotificationFailureCount(NotificationEntity notification) {
        Integer attempts = notification.getAttempts();
        if (attempts == null) {
            attempts = 0;
        }
        attempts++;
        if (attempts <= maxRetry) {
            notification.setAttempts(attempts);
            notification.setStatus(NotificationStatus.PROCESSING);
            log.info("Notification has failed to send, retrying ID: {}", notification.getId());
        } else {
            updateNotificationStatus(notification, NotificationStatus.FAILED);
            log.error("Notification ID {} has fully failed", notification.getId());
        }
        notificationRepo.saveAndFlush(notification);
    }

}

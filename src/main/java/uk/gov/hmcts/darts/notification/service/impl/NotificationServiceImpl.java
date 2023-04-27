package uk.gov.hmcts.darts.notification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.notification.dto.CreateNotificationReq;
import uk.gov.hmcts.darts.notification.entity.Notification;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;
import uk.gov.hmcts.darts.notification.service.NotificationService;

import java.sql.Timestamp;


@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;

    @Override
    public Notification sendNotification(CreateNotificationReq createNotificationReq) {
        Notification dbNotification = new Notification();
        dbNotification.setEventId(createNotificationReq.getEventId());
        dbNotification.setCaseId(createNotificationReq.getCaseId());
        dbNotification.setEmailAddress(createNotificationReq.getEmailAddress());
        dbNotification.setStatus(String.valueOf(NotificationStatus.OPEN));
        dbNotification.setAttempts(0);
        dbNotification.setTemplateValues(createNotificationReq.getTemplateValues());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dbNotification.setCreatedDateTime(now);
        dbNotification.setLastUpdatedDateTime(now);

        return notificationRepo.saveAndFlush(dbNotification);


    }

}

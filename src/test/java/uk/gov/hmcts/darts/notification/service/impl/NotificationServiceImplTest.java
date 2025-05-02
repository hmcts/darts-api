package uk.gov.hmcts.darts.notification.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.NotificationRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.helper.GovNotifyRequestHelper;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.notification.service.GovNotifyService;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private SystemUserHelper systemUserHelper;
    @Mock
    private NotificationRepository notificationRepo;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private GovNotifyService govNotifyService;
    @Mock
    private TemplateIdHelper templateIdHelper;
    @Mock
    private GovNotifyRequestHelper govNotifyRequestHelper;
    @Mock
    private LogApi logApi;

    @BeforeEach
    void setUp() {
        List<NotificationEntity> notificationEntities = someListOfNotifications();
        lenient().when(notificationRepo.findIdsByStatusIn(anyList()))
            .thenReturn(notificationEntities.stream().map(NotificationEntity::getId).toList());
        notificationEntities.forEach(
            notificationEntity ->
                lenient().when(notificationRepo.findById(notificationEntity.getId()))
                    .thenReturn(java.util.Optional.of(notificationEntity)));
    }

    @Test
    void doesntSendNotificationWhenNotificationsDisabled() {
        NotificationServiceImpl notificationService = notificationServiceNotInAtsModeAndNotificationsDisabled();

        notificationService.sendNotificationToGovNotify();

        verifyNoInteractions(govNotifyService);
    }

    @Test
    void doesntSendNotificationWhenInAtsMode() {
        NotificationServiceImpl notificationService = notificationServiceInAtsModeAndNotificationsEnabled();

        notificationService.sendNotificationToGovNotify();

        verifyNoInteractions(govNotifyService);
    }

    @Test
    void sendsNotificationWhenNotificationsEnabledAndNotInAtsMode() throws NotificationClientException {
        NotificationServiceImpl notificationService = notificationServiceNotInAtsModeAndNotificationsEnabled();

        notificationService.sendNotificationToGovNotify();

        verify(govNotifyService, times(1)).sendNotification(any());
    }

    private NotificationServiceImpl notificationServiceNotInAtsModeAndNotificationsDisabled() {
        return buildNotificationService(false, false);
    }

    private NotificationServiceImpl notificationServiceNotInAtsModeAndNotificationsEnabled() {
        return buildNotificationService(false, true);
    }

    private NotificationServiceImpl notificationServiceInAtsModeAndNotificationsEnabled() {
        return buildNotificationService(true, true);
    }

    private NotificationServiceImpl buildNotificationService(boolean atsMode, boolean enabled) {
        return new NotificationServiceImpl(
            systemUserHelper, notificationRepo, caseRepository, logApi,
            enabled, atsMode, new NotificationServiceImpl.SendNotificationToGovNotifyNowProcessor(
            0, notificationRepo, govNotifyService, templateIdHelper, govNotifyRequestHelper, logApi));
    }

    private List<NotificationEntity> someListOfNotifications() {
        var notificationEntity = new NotificationEntity();
        notificationEntity.setId(1L);
        notificationEntity.setAttempts(0);
        notificationEntity.setEventId("some-event-id");
        return List.of(notificationEntity);
    }
}
package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;
import uk.gov.hmcts.darts.audio.service.impl.AudioTransformationServiceImpl;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SystemCommandExecutorStubImpl;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.FAILED;


@Import(SystemCommandExecutorStubImpl.class)
@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceProcessAudioRequestTest extends IntegrationBase {

    private static final String NOTIFICATION_TEMPLATE_NAME_SUCCESS = "requested_audio_is_available";
    private static final String NOTIFICATION_TEMPLATE_NAME_FAILURE = "error_processing_audio";
    private static final String EMAIL_ADDRESS = "test@test.com";

    @Autowired
    private AudioTransformationServiceProcessAudioRequestGivenBuilder given;

    @Autowired
    private AudioTransformationServiceImpl audioTransformationService;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        hearing = given.aHearingWith("1", "some-courthouse", "some-courtroom");
    }

    @ParameterizedTest
    @EnumSource(names = {"DOWNLOAD", "PLAYBACK"})
    @Transactional
    void processAudioRequestShouldSucceedAndUpdateRequestStatusToCompletedAndScheduleSuccessNotificationFor(
        AudioRequestType audioRequestType) {
        given.aMediaEntityGraph();
        var userAccountEntity = given.aUserAccount(EMAIL_ADDRESS);
        given.aMediaRequestEntityForHearingWithRequestType(
            hearing,
            audioRequestType,
            userAccountEntity
        );

        Integer mediaRequestId = given.getMediaRequestEntity().getId();

        UUID blobId = audioTransformationService.processAudioRequest(mediaRequestId);
        assertNotNull(blobId);

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .findById(mediaRequestId)
            .orElseThrow();
        assertEquals(COMPLETED, mediaRequestEntity.getStatus());

        List<NotificationEntity> scheduledNotifications = dartsDatabase.getNotificationRepository()
            .findAll();
        assertEquals(1, scheduledNotifications.size());

        var notificationEntity = scheduledNotifications.get(0);
        assertEquals(NOTIFICATION_TEMPLATE_NAME_SUCCESS, notificationEntity.getEventId());
        assertNull(notificationEntity.getTemplateValues());
        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());
    }

    @ParameterizedTest
    @EnumSource(names = {"DOWNLOAD", "PLAYBACK"})
    @Transactional
    void processAudioRequestShouldFailAndUpdateRequestStatusToFailedAndScheduleFailureNotificationFor(
        AudioRequestType audioRequestType) {
        var userAccountEntity = given.aUserAccount(EMAIL_ADDRESS);
        given.aMediaRequestEntityForHearingWithRequestType(
            hearing,
            audioRequestType,
            userAccountEntity
        );

        Integer mediaRequestId = given.getMediaRequestEntity().getId();
        var exception = assertThrows(
            DartsApiException.class,
            () -> audioTransformationService.processAudioRequest(mediaRequestId)
        );

        assertEquals("Failed to process audio request", exception.getMessage());

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .findById(mediaRequestId)
            .orElseThrow();
        assertEquals(FAILED, mediaRequestEntity.getStatus());

        List<NotificationEntity> scheduledNotifications = dartsDatabase.getNotificationRepository()
            .findAll();
        assertEquals(1, scheduledNotifications.size());

        var notificationEntity = scheduledNotifications.get(0);
        assertEquals(NOTIFICATION_TEMPLATE_NAME_FAILURE, notificationEntity.getEventId());
        assertNull(notificationEntity.getTemplateValues());
        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());
    }

}

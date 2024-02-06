package uk.gov.hmcts.darts.audio.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SystemCommandExecutorStubImpl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REQUEST_ID;


@Import(SystemCommandExecutorStubImpl.class)
@Slf4j
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class AudioTransformationServiceHandleKedaInvocationForMediaRequestsTest extends IntegrationBase {

    public static final LocalDate MOCK_HEARING_DATE = LocalDate.of(2023, 5, 1);
    public static final String MOCK_HEARING_DATE_FORMATTED = "1st May 2023";
    public static final String MOCK_COURTHOUSE_NAME = "some-courthouse";
    public static final String NO_DEFENDANTS = "There are no defendants for this hearing";
    public static final String TIME_12_00 = "12:00:00";
    public static final String TIME_13_00 = "13:00:00";
    public static final String NOT_AVAILABLE = "N/A";
    private static final String EMAIL_ADDRESS = "test@test.com";
    private static final String MOCK_PLAYBACK_REQUEST_ID = "1";
    private static final String MOCK_DOWNLOAD_REQUEST_ID = "2";
    @Autowired
    private AudioTransformationServiceHandleKedaInvocationForMediaRequestsGivenBuilder given;

    @Autowired
    private AudioTransformationService audioTransformationService;

    @SpyBean
    private MediaRequestService mediaRequestService;

    private HearingEntity hearing;

    private static Map<String, String> getTemplateValuesMap(NotificationEntity notificationEntity) {
        String templateValues = notificationEntity.getTemplateValues();

        templateValues = templateValues != null
              ? templateValues.replace("{\"", "").replace("\"}", "") : "";

        return Arrays.stream(templateValues
                                   .split("\",\""))
              .map(kv -> kv.split("\":\""))
              .filter(kvArray -> kvArray.length == 2)
              .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

    @BeforeEach
    void setUp() {
        dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        hearing = given.aHearingWith("T202304130121", "some-courthouse", "some-courtroom", MOCK_HEARING_DATE);
    }

    @Transactional
    @Test
    @SuppressWarnings("PMD.LawOfDemeter")
    public void handleKedaInvocationForMediaRequestsShouldSucceedAndUpdateRequestStatusToCompletedAndScheduleSuccessNotificationForDownload() {
        given.aMediaEntityGraph();
        var userAccountEntity = given.aUserAccount(EMAIL_ADDRESS);
        given.aMediaRequestEntityForHearingWithRequestType(
              hearing,
              AudioRequestType.DOWNLOAD,
              userAccountEntity
        );

        Integer mediaRequestId = given.getMediaRequestEntity().getId();

        audioTransformationService.handleKedaInvocationForMediaRequests();

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
              .findById(mediaRequestId)
              .orElseThrow();
        assertEquals(COMPLETED, mediaRequestEntity.getStatus());

        List<NotificationEntity> scheduledNotifications = dartsDatabase.getNotificationRepository()
              .findAll();
        assertEquals(1, scheduledNotifications.size());

        var notificationEntity = scheduledNotifications.get(0);
        assertEquals(NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString(), notificationEntity.getEventId());
        assertNull(notificationEntity.getTemplateValues());
        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());
    }

    @Test
    @Transactional
    @SuppressWarnings("PMD.LawOfDemeter")
    public void handleKedaInvocationForMediaRequestsShouldSucceedAndUpdateRequestStatusToCompletedAndScheduleSuccessNotificationForPlayback() {
        given.aMediaEntityGraph();
        var userAccountEntity = given.aUserAccount(EMAIL_ADDRESS);
        given.aMediaRequestEntityForHearingWithRequestType(
              hearing,
              AudioRequestType.PLAYBACK,
              userAccountEntity
        );

        Integer mediaRequestId = given.getMediaRequestEntity().getId();

        audioTransformationService.handleKedaInvocationForMediaRequests();

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
              .findById(mediaRequestId)
              .orElseThrow();
        assertEquals(COMPLETED, mediaRequestEntity.getStatus());

        List<NotificationEntity> scheduledNotifications = dartsDatabase.getNotificationRepository()
              .findAll();
        assertEquals(1, scheduledNotifications.size());

        var notificationEntity = scheduledNotifications.get(0);
        assertEquals(NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString(), notificationEntity.getEventId());
        assertNull(notificationEntity.getTemplateValues());
        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());
    }

    @ParameterizedTest
    @EnumSource(names = {"DOWNLOAD", "PLAYBACK"})
    @Transactional
    @SuppressWarnings("PMD.LawOfDemeter")
    public void handleKedaInvocationForMediaRequestsShouldFailAndUpdateRequestStatusToFailedAndScheduleFailureNotificationFor(
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
              () -> audioTransformationService.handleKedaInvocationForMediaRequests()
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
        assertEquals(NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString(), notificationEntity.getEventId());

        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());

        Map<String, String> templateParams = getTemplateValuesMap(notificationEntity);

        assertEquals(TIME_12_00, templateParams.get(AUDIO_START_TIME));
        assertEquals(TIME_13_00, templateParams.get(AUDIO_END_TIME));
        assertEquals(MOCK_HEARING_DATE_FORMATTED, templateParams.get(HEARING_DATE));
        assertEquals(MOCK_COURTHOUSE_NAME, templateParams.get(COURTHOUSE));
        assertEquals(NO_DEFENDANTS, templateParams.get(DEFENDANTS));

        String requestId = "1".equals(templateParams.get(REQUEST_ID)) ? "2" : "1";
        if ("1".equals(requestId)) {
            assertEquals(MOCK_PLAYBACK_REQUEST_ID, requestId);
        }
        if ("2".equals(requestId)) {
            assertEquals(MOCK_DOWNLOAD_REQUEST_ID, requestId);
        }
    }

    @ParameterizedTest
    @EnumSource(names = {"DOWNLOAD", "PLAYBACK"})
    @Transactional
    public void handleKedaInvocationForMediaRequestsShouldNotInvokeProcessAudioRequestWhenNoOpenMediaRequestsExist(
          AudioRequestType audioRequestType) {
        given.aUserAccount(EMAIL_ADDRESS);

        audioTransformationService.handleKedaInvocationForMediaRequests();

        verify(mediaRequestService, never()).updateAudioRequestStatus(any(), any());
    }
}

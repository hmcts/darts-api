package uk.gov.hmcts.darts.audio.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REQUEST_ID;

@TestPropertySource(properties = {"darts.audio.transformation.service.audio.file=tests/audio/WithViqHeader/viq0001min.mp2"})
@Slf4j
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class AudioTransformationServiceHandleKedaInvocationForMediaRequestsTest extends PostgresIntegrationBase {

    private static final String EMAIL_ADDRESS = "test@test.com";
    public static final LocalDateTime MOCK_HEARING_DATE = LocalDateTime.of(2023, 5, 1, 10, 0, 0);
    public static final String MOCK_HEARING_DATE_FORMATTED = "1st May 2023";
    public static final String MOCK_COURTHOUSE_NAME = "SOME-COURTHOUSE";
    public static final String NO_DEFENDANTS = "There are no defendants for this hearing";
    private static final String MOCK_PLAYBACK_REQUEST_ID = "1";
    private static final String MOCK_DOWNLOAD_REQUEST_ID = "2";
    public static final String TIME_12_00 = "12:00:00";
    public static final String TIME_13_00 = "13:00:00";
    private static final OffsetDateTime TIME_20_00 = OffsetDateTime.parse("2023-01-01T20:00Z");
    private static final OffsetDateTime TIME_20_30 = OffsetDateTime.parse("2023-01-01T20:30Z");
    private static final OffsetDateTime TIME_12_01 = OffsetDateTime.parse("2023-01-01T12:01Z");
    private static final OffsetDateTime TIME_14_00 = OffsetDateTime.parse("2023-01-01T14:00Z");
    private static final String ONE = "1";
    private static final String TWO = "2";

    @Autowired
    private AudioTransformationServiceHandleKedaInvocationForMediaRequestsGivenBuilder given;

    @Autowired
    private AudioTransformationService audioTransformationService;

    @MockitoSpyBean
    private MediaRequestService mediaRequestService;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private HearingEntity hearing;


    @BeforeEach
    void setUp() {
        dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        hearing = given.aHearingWith("T202304130121", "SOME-COURTHOUSE", "some-courtroom", MOCK_HEARING_DATE);

        UserAccountEntity testUser = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

    }

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

        var notificationEntity = scheduledNotifications.getFirst();
        assertEquals(NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString(), notificationEntity.getEventId());
        assertNull(notificationEntity.getTemplateValues());
        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());
    }

    @Test
    @SuppressWarnings("PMD.LawOfDemeter")
    public void handleKedaInvocationForMediaRequestsShouldSucceedAndUpdateRequestStatusToCompletedOnlyOnce() {
        given.aMediaEntityGraph();
        var userAccountEntity = given.aUserAccount(EMAIL_ADDRESS);
        // request time includes gap in audio, resulting in multiple generated files
        given.aMediaRequestEntityForHearingWithRequestType(
            hearing,
            AudioRequestType.PLAYBACK,
            userAccountEntity,
            TIME_12_01,
            TIME_14_00
        );

        audioTransformationService.handleKedaInvocationForMediaRequests();

        // checking that the media request is only updated to COMPLETED once
        verify(mediaRequestService, times(1)).updateAudioRequestCompleted(any(MediaRequestEntity.class));
    }

    @Test
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

        var notificationEntity = scheduledNotifications.getFirst();
        assertEquals(NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString(), notificationEntity.getEventId());
        assertNull(notificationEntity.getTemplateValues());
        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());
    }

    @Test
    @SuppressWarnings("PMD.LawOfDemeter")
    public void handleKedaInvocationForMediaRequestsShouldResetRequestStatusToOpen() {
        given.aMediaEntityGraph();
        var userAccountEntity = given.aUserAccount(EMAIL_ADDRESS);
        given.aMediaRequestEntityForHearingWithRequestType(
            hearing,
            AudioRequestType.PLAYBACK,
            userAccountEntity,
            TIME_20_00,
            TIME_20_30
        );

        Integer mediaRequestId = given.getMediaRequestEntity().getId();

        audioTransformationService.handleKedaInvocationForMediaRequests();

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .findById(mediaRequestId)
            .orElseThrow();
        assertEquals(OPEN, mediaRequestEntity.getStatus());
    }

    @ParameterizedTest
    @EnumSource(names = {"DOWNLOAD", "PLAYBACK"})
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

        audioTransformationService.handleKedaInvocationForMediaRequests();

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .findById(mediaRequestId)
            .orElseThrow();
        assertEquals(FAILED, mediaRequestEntity.getStatus());

        List<NotificationEntity> scheduledNotifications = dartsDatabase.getNotificationRepository()
            .findAll();
        assertEquals(1, scheduledNotifications.size());

        var notificationEntity = scheduledNotifications.getFirst();
        assertEquals(NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString(), notificationEntity.getEventId());

        assertEquals(NotificationStatus.OPEN, notificationEntity.getStatus());
        assertEquals(EMAIL_ADDRESS, notificationEntity.getEmailAddress());

        Map<String, String> templateParams = getTemplateValuesMap(notificationEntity);

        assertEquals(TIME_12_00, templateParams.get(AUDIO_START_TIME));
        assertEquals(TIME_13_00, templateParams.get(AUDIO_END_TIME));
        assertEquals(MOCK_HEARING_DATE_FORMATTED, templateParams.get(HEARING_DATE));
        assertEquals(MOCK_COURTHOUSE_NAME, templateParams.get(COURTHOUSE));
        assertEquals(NO_DEFENDANTS, templateParams.get(DEFENDANTS));

        String requestId = ONE.equals(templateParams.get(REQUEST_ID)) ? TWO : ONE;
        if (ONE.equals(requestId)) {
            assertEquals(MOCK_PLAYBACK_REQUEST_ID, requestId);
        }
        if (TWO.equals(requestId)) {
            assertEquals(MOCK_DOWNLOAD_REQUEST_ID, requestId);
        }
    }

    @ParameterizedTest
    @EnumSource(names = {"DOWNLOAD", "PLAYBACK"})
    public void handleKedaInvocationForMediaRequestsShouldNotInvokeProcessAudioRequestWhenNoOpenMediaRequestsExist(
        AudioRequestType audioRequestType) {
        given.aUserAccount(EMAIL_ADDRESS);

        audioTransformationService.handleKedaInvocationForMediaRequests();

        verify(mediaRequestService, never()).updateAudioRequestStatus(any(MediaRequestEntity.class), any());
    }

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
}
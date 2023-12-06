package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;

@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceImplTest {
    private static final OffsetDateTime TIME_11_59 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_01 = OffsetDateTime.parse("2023-01-01T12:01Z");
    private static final OffsetDateTime TIME_12_20 = OffsetDateTime.parse("2023-01-01T12:20Z");
    private static final OffsetDateTime TIME_12_21 = OffsetDateTime.parse("2023-01-01T12:21Z");
    private static final OffsetDateTime TIME_12_39 = OffsetDateTime.parse("2023-01-01T12:39Z");
    private static final OffsetDateTime TIME_12_40 = OffsetDateTime.parse("2023-01-01T12:40Z");
    private static final OffsetDateTime TIME_12_59 = OffsetDateTime.parse("2023-01-01T12:59Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");
    private static final OffsetDateTime TIME_13_01 = OffsetDateTime.parse("2023-01-01T13:01Z");


    private static final UUID BLOB_LOCATION = UUID.randomUUID();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    public static final String NO_DEFENDANTS = "There are no defendants for this hearing";
    public static final String MOCK_DEFENDANT_NAME = "Any Defendant";
    public static final String NOT_AVAILABLE = "N/A";
    public static final LocalDate MOCK_HEARING_DATE = LocalDate.of(2023, 5, 1);
    public static final String MOCK_COURTHOUSE_NAME = "mockCourtHouse";

    @Mock
    private DataManagementApi mockDataManagementApi;

    @Mock
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private AudioTransformationServiceImpl audioTransformationService;

    @Mock
    private MediaRequestServiceImpl mockMediaRequestService;

    @Mock
    private MediaRequestEntity mockMediaRequestEntity;

    @Mock
    private HearingEntity mockHearing;

    @Mock
    private CourtCaseEntity mockCourtCase;

    @Mock
    private UserAccountEntity mockUserAccountEntity;

    @Mock
    private UserAccountRepository mockUserAccountRepository;

    @Mock
    private CourthouseEntity mockCourthouse;

    @Mock
    private NotificationApi mockNotificationApi;

    @Mock
    private DefendantEntity mockDefendantEntity;

    @Captor
    private ArgumentCaptor<SaveNotificationToDbRequest> dbNotificationRequestCaptor;


    @Test
    void testGetAudioBlobData() {
        when(mockDataManagementApi.getBlobDataFromUnstructuredContainer(BLOB_LOCATION))
            .thenReturn(BINARY_DATA);

        BinaryData binaryData = audioTransformationService.getUnstructuredAudioBlob(BLOB_LOCATION);
        assertEquals(BINARY_DATA, binaryData);
    }

    @Test
    void getMediaMetadataShouldReturnRepositoryResultsUnmodifiedWhenRepositoryHasResult() {
        List<MediaEntity> expectedResults = Collections.singletonList(new MediaEntity());

        when(mediaRepository.findAllByHearingId(any()))
            .thenReturn(expectedResults);

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(1);

        assertEquals(expectedResults, mediaEntities);
    }

    @Test
    void getMediaMetadataShouldReturnRepositoryResultsUnmodifiedWhenRepositoryResultIsEmpty() {
        List<MediaEntity> expectedResults = Collections.emptyList();

        when(mediaRepository.findAllByHearingId(any()))
            .thenReturn(expectedResults);

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(1);

        assertEquals(expectedResults, mediaEntities);
    }

    @Test
    void saveProcessedDataShouldSaveBlobAndSetStatus() {
        final MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        final MediaRequestEntity mediaRequestEntityUpdated = new MediaRequestEntity();
        mediaRequestEntityUpdated.setStatus(COMPLETED);

        when(mockDataManagementApi.saveBlobDataToOutboundContainer(any()))
            .thenReturn(BLOB_LOCATION);

        when(mockTransientObjectDirectoryService.saveTransientDataLocation(
            any(),
            any()
        )).thenReturn(mockTransientObjectDirectoryEntity);

        audioTransformationService.saveProcessedData(
            mediaRequestEntity,
            BINARY_DATA
        );

        verify(mockDataManagementApi).saveBlobDataToOutboundContainer(BINARY_DATA);

        verify(mockTransientObjectDirectoryService).saveTransientDataLocation(mediaRequestEntity, BLOB_LOCATION);
    }

    @Test
    void testHandleKedaInvocationForMediaRequestsRequestTypeNull() {

        when(mockMediaRequestEntity.getId()).thenReturn(1);
        when(mockMediaRequestService.getOldestMediaRequestByStatus(AudioRequestStatus.OPEN))
            .thenReturn(Optional.of(mockMediaRequestEntity));
        when(mockMediaRequestService.getMediaRequestById(1)).thenReturn(mockMediaRequestEntity);
        when(mockMediaRequestEntity.getHearing()).thenReturn(mockHearing);

        assertThrows(NullPointerException.class, () -> audioTransformationService.handleKedaInvocationForMediaRequests());
    }

    @Test
    void testHandleKedaInvocationForMediaRequestsCaseNull() {
        when(mockMediaRequestEntity.getId()).thenReturn(1);
        when(mockMediaRequestService.getOldestMediaRequestByStatus(AudioRequestStatus.OPEN))
            .thenReturn(Optional.of(mockMediaRequestEntity));
        when(mockMediaRequestService.getMediaRequestById(1)).thenReturn(mockMediaRequestEntity);
        when(mockMediaRequestEntity.getHearing()).thenReturn(mockHearing);
        when(mockMediaRequestEntity.getRequestType()).thenReturn(DOWNLOAD);

        assertThrows(NullPointerException.class, () -> audioTransformationService.handleKedaInvocationForMediaRequests());

    }

    @Test
    void filterMediaByMediaRequestDatesWithStartDateExactRequestAndEndDateExactRequest() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_00);
        MediaRequestEntity mediaRequestEntity = createMediaRequest(TIME_12_00, TIME_13_00);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestDates(mediaEntities, mediaRequestEntity);

        assertEquals(3, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.get(0).getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(2).getEnd());
    }

    @Test
    void filterMediaByMediaRequestDatesWithStartDateAfterRequestAndEndDateBeforeRequest() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_01, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_12_59);
        MediaRequestEntity mediaRequestEntity = createMediaRequest(TIME_12_00, TIME_13_00);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestDates(mediaEntities, mediaRequestEntity);
        assertEquals(3, mediaEntitiesResult.size());
        assertEquals(TIME_12_01, mediaEntitiesResult.get(0).getStart());
        assertEquals(TIME_12_59, mediaEntitiesResult.get(2).getEnd());
    }

    @Test
    void filterMediaByMediaRequestDatesWithStartDateBeforeRequestAndEndDateAfterRequest() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_11_59, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_01);
        MediaRequestEntity mediaRequestEntity = createMediaRequest(TIME_12_00, TIME_13_00);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestDates(mediaEntities, mediaRequestEntity);
        assertEquals(3, mediaEntitiesResult.size());
        assertEquals(TIME_11_59, mediaEntitiesResult.get(0).getStart());
        assertEquals(TIME_13_01, mediaEntitiesResult.get(2).getEnd());
    }

    @Test
    void filterMediaByMediaRequestDatesWithStartDateAndEndDateExactMiddleMediaMatch() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_11_59, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_01);
        MediaRequestEntity mediaRequestEntity = createMediaRequest(TIME_12_20, TIME_12_40);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestDates(mediaEntities, mediaRequestEntity);
        assertEquals(1, mediaEntitiesResult.size());
        assertEquals(TIME_12_20, mediaEntitiesResult.get(0).getStart());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(0).getEnd());
    }

    @Test
    void filterMediaByMediaRequestDatesWithStartDateBetweenRequestAndEndDateBetweenRequest() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_00);
        MediaRequestEntity mediaRequestEntity = createMediaRequest(TIME_12_21, TIME_12_39);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestDates(mediaEntities, mediaRequestEntity);
        assertEquals(1, mediaEntitiesResult.size());
        assertEquals(TIME_12_20, mediaEntitiesResult.get(0).getStart());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(0).getEnd());
    }

    @Test
    void filterMediaByMediaRequestDatesWithRequestStartAndEndDateOutSideMediaRange() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_12_59);
        MediaRequestEntity mediaRequestEntity = createMediaRequest(TIME_13_00, TIME_13_01);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestDates(mediaEntities, mediaRequestEntity);
        assertEquals(0, mediaEntitiesResult.size());
    }

    private List<MediaEntity> createMediaEntities(OffsetDateTime startTime1, OffsetDateTime endTime1,
                                                  OffsetDateTime startTime2, OffsetDateTime endTime2,
                                                  OffsetDateTime startTime3, OffsetDateTime endTime3) {
        List<MediaEntity> mediaEntities = new ArrayList<>();
        mediaEntities.add(createMediaEntity(startTime1, endTime1));
        mediaEntities.add(createMediaEntity(startTime2, endTime2));
        mediaEntities.add(createMediaEntity(startTime3, endTime3));
        return mediaEntities;
    }

    private MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime) {
        MediaEntity media = new MediaEntity();
        media.setStart(startTime);
        media.setEnd(endTime);
        return media;
    }

    private MediaRequestEntity createMediaRequest(OffsetDateTime startTime, OffsetDateTime endTime) {
        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setStartTime(startTime);
        mediaRequest.setEndTime(endTime);
        return mediaRequest;
    }

    @Test
    void testNotifyUserScheduleErrorNotificationNoDefendants() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);

        audioTransformationService.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getTemplateValues().get(DEFENDANTS), NO_DEFENDANTS);
    }

    @Test
    void testNotifyUserScheduleErrorNotificationWithDefendants() {
        List<DefendantEntity> defendantList = new ArrayList<>();
        defendantList.add(mockDefendantEntity);

        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);
        when(mockDefendantEntity.getName()).thenReturn(MOCK_DEFENDANT_NAME);
        when(mockCourtCase.getDefendantList()).thenReturn(defendantList);

        audioTransformationService.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getTemplateValues().get(DEFENDANTS), MOCK_DEFENDANT_NAME);

    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingCourthouseName() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, null, TIME_12_00, TIME_13_00);

        audioTransformationService.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getTemplateValues().get(COURTHOUSE), NOT_AVAILABLE);
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingHearingData() {
        initNotifyUserScheduleErrorNotificationMocks(null, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);

        audioTransformationService.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getTemplateValues().get(HEARING_DATE), NOT_AVAILABLE);
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingNoStartTime() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, null, TIME_13_00);

        audioTransformationService.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getTemplateValues().get(AUDIO_START_TIME), NOT_AVAILABLE);
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingNoEndTime() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, null);

        audioTransformationService.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(actual.getTemplateValues().get(AUDIO_END_TIME), NOT_AVAILABLE);
    }

    private void initNotifyUserScheduleErrorNotificationMocks(LocalDate hearingDate, String courthouseName, OffsetDateTime startTime, OffsetDateTime endTime) {
        when(mockMediaRequestEntity.getId()).thenReturn(1);
        when(mockMediaRequestEntity.getHearing()).thenReturn(mockHearing);
        when(mockMediaRequestEntity.getStartTime()).thenReturn(startTime);
        when(mockMediaRequestEntity.getEndTime()).thenReturn(endTime);
        when(mockMediaRequestEntity.getRequestor()).thenReturn(mockUserAccountEntity);
        when(mockUserAccountEntity.getId()).thenReturn(1);
        when(mockHearing.getHearingDate()).thenReturn(hearingDate);
        when(mockHearing.getCourtCase()).thenReturn(mockCourtCase);
        when(mockCourtCase.getCourthouse()).thenReturn(mockCourthouse);
        when(mockCourthouse.getCourthouseName()).thenReturn(courthouseName);
        when(mockUserAccountRepository.findById(any())).thenReturn(Optional.of(mockUserAccountEntity));
    }
}

package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioTransformationServiceProperties;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.helper.TransformedMediaHelper;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponseImpl;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;

@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceImplTest {
    private static final OffsetDateTime TIME_09_59 = OffsetDateTime.parse("2023-01-01T09:59Z");
    private static final OffsetDateTime TIME_10_00 = OffsetDateTime.parse("2023-01-01T10:00Z");
    private static final OffsetDateTime TIME_10_05 = OffsetDateTime.parse("2023-01-01T11:05Z");
    private static final OffsetDateTime TIME_10_10 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_10_15 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_10_20 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_11_59 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_01 = OffsetDateTime.parse("2023-01-01T12:01Z");
    private static final OffsetDateTime TIME_12_20 = OffsetDateTime.parse("2023-01-01T12:20Z");
    private static final OffsetDateTime TIME_12_20_00_900 = OffsetDateTime.parse("2023-01-01T12:20:00.900Z");
    private static final OffsetDateTime TIME_12_21 = OffsetDateTime.parse("2023-01-01T12:21Z");
    private static final OffsetDateTime TIME_12_39 = OffsetDateTime.parse("2023-01-01T12:39Z");
    private static final OffsetDateTime TIME_12_40 = OffsetDateTime.parse("2023-01-01T12:40Z");
    private static final OffsetDateTime TIME_12_59 = OffsetDateTime.parse("2023-01-01T12:59Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");
    private static final OffsetDateTime TIME_13_01 = OffsetDateTime.parse("2023-01-01T13:01Z");
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    public static final String NO_DEFENDANTS = "There are no defendants for this hearing";
    public static final String MOCK_DEFENDANT_NAME = "Any Defendant";
    public static final String MOCK_DEFENDANT_LIST = "Any Defendant, Any Defendant";
    public static final String NOT_AVAILABLE = "N/A";
    public static final LocalDate MOCK_HEARING_DATE = LocalDate.of(2023, 1, 1);
    public static final LocalDate MOCK_HEARING_DATE_BST = LocalDate.of(2023, 7, 1);
    public static final String MOCK_HEARING_DATE_FORMATTED = "1st January 2023";
    public static final String MOCK_HEARING_DATE_BST_FORMATTED = "1st July 2023";
    public static final String MOCK_COURTHOUSE_NAME = "mockCourtHouse";
    public static final int MOCK_CASEID = 99;
    public static final String TEST_EXTENSION = AudioRequestOutputFormat.MP3.getExtension();
    public static final String TEST_FILE_NAME = "case1_23_Nov_2023" + "." + TEST_EXTENSION;

    @Mock
    private DataManagementApi mockDataManagementApi;

    @Mock
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @InjectMocks
    private AudioTransformationServiceImpl audioTransformationService;

    @InjectMocks
    private TransformedMediaHelper transformedMediaHelper;

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
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private AudioTransformationServiceProperties audioTransformationServiceProperties;
    @Captor
    private ArgumentCaptor<SaveNotificationToDbRequest> dbNotificationRequestCaptor;

    @BeforeEach
    void beforeEach() {
        lenient().when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        lenient().when(audioTransformationServiceProperties.getLoopCutoffMinutes()).thenReturn(15);
    }

    @Test
    void getMediaByHearingIdShouldReturnRepositoryResultsUnmodifiedWhenRepositoryHasResult() {
        List<MediaEntity> expectedResults = Collections.singletonList(new MediaEntity());

        when(mediaRepository.findAllCurrentMediaByHearingId(any(), anyBoolean()))
            .thenReturn(expectedResults);

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaByHearingId(1);

        assertEquals(expectedResults, mediaEntities);
    }

    @Test
    void getMediaByHearingIdShouldReturnRepositoryResultsUnmodifiedWhenRepositoryResultIsEmpty() {
        List<MediaEntity> expectedResults = Collections.emptyList();

        when(mediaRepository.findAllCurrentMediaByHearingId(any(), anyBoolean()))
            .thenReturn(expectedResults);

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaByHearingId(1);

        assertEquals(expectedResults, mediaEntities);
    }

    @Test
    void saveProcessedData_shouldSaveBlobAndSetStatus() {
        final MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setRequestType(DOWNLOAD);
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(1);
        mediaRequestEntity.setCreatedBy(userAccount);

        BlobClientUploadResponse blobClientUploadResponse = mock(BlobClientUploadResponseImpl.class);
        String blobName = UUID.randomUUID().toString();
        when(blobClientUploadResponse.getBlobName())
            .thenReturn(blobName);
        when(blobClientUploadResponse.getBlobSize())
            .thenReturn(1000L);

        when(mockDataManagementApi.saveBlobToContainer(any(), any(), any()))
            .thenReturn(blobClientUploadResponse);

        when(mockTransientObjectDirectoryService.saveTransientObjectDirectoryEntity(
            any(),
            any()
        )).thenReturn(mockTransientObjectDirectoryEntity);

        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(1);
        doAnswer(invocation -> invocation.getArgument(0)).when(transformedMediaRepository).save(any());

        when(mockTransientObjectDirectoryEntity.getTransformedMedia())
            .thenReturn(transformedMediaEntity);

        when(mockTransientObjectDirectoryEntity.getTransformedMedia(
        )).thenReturn(transformedMediaEntity);

        AudioFileInfo audioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_00.toInstant())
            .endTime(TIME_12_20.toInstant())
            .channel(0)
            .path(Path.of("test/b6b51cb7-9ff8-44de-bf53-62c2bd2e13f3.zip"))
            .build();

        String returnedBlobName = transformedMediaHelper.saveToStorage(
            mediaRequestEntity,
            new ByteArrayInputStream(TEST_BINARY_STRING.getBytes()), "filename",
            audioFileInfo
        );

        verify(mockDataManagementApi).saveBlobToContainer(any(), eq(DatastoreContainerType.OUTBOUND), any());
        verify(mockTransientObjectDirectoryService).saveTransientObjectDirectoryEntity(any(TransformedMediaEntity.class), eq(blobName));
        assertEquals(blobName, returnedBlobName);
    }

    @Test
    void testHandleKedaInvocationForMediaRequestsRequestTypeNull() {
        when(mockMediaRequestEntity.getId()).thenReturn(1);
        when(mockMediaRequestEntity.getStatus()).thenReturn(PROCESSING);
        when(mockMediaRequestService.retrieveMediaRequestForProcessing(new ArrayList<>())).thenReturn(Optional.of(mockMediaRequestEntity));
        when(mockMediaRequestEntity.getHearing()).thenReturn(mockHearing);

        assertThrows(NullPointerException.class, () -> audioTransformationService.handleKedaInvocationForMediaRequests());
    }

    @Test
    void testHandleKedaInvocationForMediaRequestsCaseNull() {
        when(mockMediaRequestEntity.getId()).thenReturn(1);
        when(mockMediaRequestEntity.getStatus()).thenReturn(PROCESSING);
        when(mockMediaRequestEntity.getRequestType()).thenReturn(DOWNLOAD);
        when(mockMediaRequestService.retrieveMediaRequestForProcessing(new ArrayList<>())).thenReturn(Optional.of(mockMediaRequestEntity));
        when(mockMediaRequestEntity.getHearing()).thenReturn(mockHearing);

        assertThrows(NullPointerException.class, () -> audioTransformationService.handleKedaInvocationForMediaRequests());

    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsMediaWithStartDateAndEndDateTheSame() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_20, TIME_12_40, TIME_13_00);
        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(2, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getEnd());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(1).getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(1).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsMediaWithStartDateAndEndDateLessThan1Second() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_20_00_900, TIME_12_40, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(2, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getEnd());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(1).getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(1).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsOnlyMediaWithStartDateAndEndDateTheSame() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_00, TIME_12_20, TIME_12_20, TIME_13_00, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(0, mediaEntitiesResult.size());
    }


    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsMediaWithStartDateAfterEndDate() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_40, TIME_12_20, TIME_12_40, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(2, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getEnd());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(1).getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(1).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsOnlyMediaWithStartDateAfterEndDate() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_20, TIME_12_00, TIME_12_40, TIME_12_20, TIME_13_00, TIME_12_40);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(0, mediaEntitiesResult.size());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsMediaWithNullStartDate() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, null, TIME_12_20, TIME_12_40, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(2, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getEnd());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(1).getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(1).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsOnlyMediaWithNullStartDates() {
        List<MediaEntity> mediaEntities = createMediaEntities(null, TIME_12_00, null, TIME_12_20, null, TIME_12_40);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        assertEquals(0, mediaEntitiesResult.size());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsMediaWithNullEndDate() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_40, null, TIME_12_40, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(2, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getEnd());
        assertEquals(TIME_12_40, mediaEntitiesResult.get(1).getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(1).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WhichContainsOnlyMediaNullEndDates() {
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_20, null, TIME_12_40, null, TIME_13_00, null);
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        assertEquals(0, mediaEntitiesResult.size());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithStartDateExactRequestAndEndDateExactRequest() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(3, mediaEntitiesResult.size());
        assertEquals(TIME_12_00, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_13_00, mediaEntitiesResult.get(2).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithStartDateAfterRequestAndEndDateBeforeRequest() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_01, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_12_59);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(3, mediaEntitiesResult.size());
        assertEquals(TIME_12_01, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_59, mediaEntitiesResult.get(2).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithStartDateBeforeRequestAndEndDateAfterRequest() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_11_59, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_01);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_00,
            TIME_13_00
        );

        // then
        assertEquals(3, mediaEntitiesResult.size());
        assertEquals(TIME_11_59, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_13_01, mediaEntitiesResult.get(2).getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithStartDateAndEndDateExactMiddleMediaMatch() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_11_59, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_01);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_20,
            TIME_12_40
        );

        // then
        assertEquals(1, mediaEntitiesResult.size());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_40, mediaEntitiesResult.getFirst().getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithStartDateBetweenRequestAndEndDateBetweenRequest() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_13_00);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_12_21,
            TIME_12_39
        );

        // then
        assertEquals(1, mediaEntitiesResult.size());
        assertEquals(TIME_12_20, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_12_40, mediaEntitiesResult.getFirst().getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithRequestStartAndEndDateOutSideMediaRange() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_12_00, TIME_12_20, TIME_12_20, TIME_12_40, TIME_12_40, TIME_12_59);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_13_00,
            TIME_13_01
        );

        // then
        assertEquals(0, mediaEntitiesResult.size());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithRequestStartTimeMatchesAnAudioEndTime() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_09_59, TIME_10_00, TIME_10_05, TIME_10_10, TIME_10_15, TIME_10_20);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_10_00,
            TIME_10_10
        );

        // then
        assertEquals(1, mediaEntitiesResult.size());
        assertEquals(TIME_10_05, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_10_10, mediaEntitiesResult.getFirst().getEnd());
    }

    @Test
    void filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel_ReturnsFilteredData_WithRequestEndTimeMatchesAnAudioStartTime() {
        // given
        List<MediaEntity> mediaEntities = createMediaEntities(TIME_09_59, TIME_10_00, TIME_10_05, TIME_10_10, TIME_10_15, TIME_10_20);

        // when
        List<MediaEntity> mediaEntitiesResult = audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntities,
            TIME_09_59,
            TIME_10_15
        );

        // then
        assertEquals(2, mediaEntitiesResult.size());
        assertEquals(TIME_09_59, mediaEntitiesResult.getFirst().getStart());
        assertEquals(TIME_10_10, mediaEntitiesResult.get(1).getEnd());
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

    @Test
    void testNotifyUserScheduleErrorNotification() {
        List<String> defendants = new ArrayList<>();
        defendants.add(MOCK_DEFENDANT_NAME);
        defendants.add(MOCK_DEFENDANT_NAME);

        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);
        when(mockCourtCase.getDefendantStringList()).thenReturn(defendants);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());
        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        assertEquals(actual.getTemplateValues().get(AUDIO_START_TIME), TIME_12_00.format(formatter));
        assertEquals(actual.getTemplateValues().get(AUDIO_END_TIME), TIME_13_00.format(formatter));
        assertEquals(MOCK_HEARING_DATE_FORMATTED, actual.getTemplateValues().get(HEARING_DATE));
        assertEquals(MOCK_COURTHOUSE_NAME, actual.getTemplateValues().get(COURTHOUSE));
        assertEquals(MOCK_DEFENDANT_LIST, actual.getTemplateValues().get(DEFENDANTS));
        assertEquals(mockUserAccountEntity, actual.getUserAccountsToEmail().getFirst());
        assertEquals(actual.getEventId(), NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());
        assertEquals(MOCK_CASEID, actual.getCaseId());
    }

    @Test
    void testNotifyUserScheduleErrorNotificationUsingBstDateTime() {
        List<String> defendants = new ArrayList<>();
        defendants.add(MOCK_DEFENDANT_NAME);
        defendants.add(MOCK_DEFENDANT_NAME);

        OffsetDateTime startDateTime = OffsetDateTime.parse("2023-07-01T12:00Z");
        OffsetDateTime endDateTime = OffsetDateTime.parse("2023-07-01T13:00Z");
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE_BST, MOCK_COURTHOUSE_NAME, startDateTime, endDateTime);
        when(mockCourtCase.getDefendantStringList()).thenReturn(defendants);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());
        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();

        assertEquals(actual.getTemplateValues().get(AUDIO_START_TIME), "13:00:00");
        assertEquals(actual.getTemplateValues().get(AUDIO_END_TIME), "14:00:00");
        assertEquals(MOCK_HEARING_DATE_BST_FORMATTED, actual.getTemplateValues().get(HEARING_DATE));
        assertEquals(MOCK_COURTHOUSE_NAME, actual.getTemplateValues().get(COURTHOUSE));
        assertEquals(MOCK_DEFENDANT_LIST, actual.getTemplateValues().get(DEFENDANTS));
        assertEquals(mockUserAccountEntity, actual.getUserAccountsToEmail().getFirst());
        assertEquals(actual.getEventId(), NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());
        assertEquals(MOCK_CASEID, actual.getCaseId());
    }

    @Test
    void testNotifyUserScheduleErrorNotificationNoDefendants() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(NO_DEFENDANTS, actual.getTemplateValues().get(DEFENDANTS));
    }

    @Test
    void testNotifyUserScheduleErrorNotificationWithADefendant() {
        List<String> defendants = new ArrayList<>();
        defendants.add(MOCK_DEFENDANT_NAME);

        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);
        when(mockCourtCase.getDefendantStringList()).thenReturn(defendants);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(MOCK_DEFENDANT_NAME, actual.getTemplateValues().get(DEFENDANTS));
    }

    @Test
    void testNotifyUserScheduleErrorNotificationWithDefendants() {
        List<String> defendants = new ArrayList<>();
        defendants.add(MOCK_DEFENDANT_NAME);
        defendants.add(MOCK_DEFENDANT_NAME);

        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);
        when(mockCourtCase.getDefendantStringList()).thenReturn(defendants);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(MOCK_DEFENDANT_LIST, actual.getTemplateValues().get(DEFENDANTS));
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingCourthouseName() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, null, TIME_12_00, TIME_13_00);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(NOT_AVAILABLE, actual.getTemplateValues().get(COURTHOUSE));
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingHearingData() {
        initNotifyUserScheduleErrorNotificationMocks(null, MOCK_COURTHOUSE_NAME, TIME_12_00, TIME_13_00);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(NOT_AVAILABLE, actual.getTemplateValues().get(HEARING_DATE));
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingNoStartTime() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, null, TIME_13_00);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(NOT_AVAILABLE, actual.getTemplateValues().get(AUDIO_START_TIME));
    }

    @Test
    void testNotifyUserScheduleErrorNotificationMissingNoEndTime() {
        initNotifyUserScheduleErrorNotificationMocks(MOCK_HEARING_DATE, MOCK_COURTHOUSE_NAME, TIME_12_00, null);

        transformedMediaHelper.notifyUser(mockMediaRequestEntity, mockCourtCase, NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());

        verify(mockNotificationApi).scheduleNotification(dbNotificationRequestCaptor.capture());
        var actual = dbNotificationRequestCaptor.getValue();
        assertEquals(NOT_AVAILABLE, actual.getTemplateValues().get(AUDIO_END_TIME));
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
        when(mockCourtCase.getId()).thenReturn(MOCK_CASEID);
        when(mockCourtCase.getCourthouse()).thenReturn(mockCourthouse);
        when(mockCourthouse.getDisplayName()).thenReturn(courthouseName);
        when(mockUserAccountRepository.findById(any())).thenReturn(Optional.of(mockUserAccountEntity));
    }

    @Test
    void getDayFormat() {
        assertEquals("st", TransformedMediaHelper.getNthNumber(1));
        assertEquals("nd", TransformedMediaHelper.getNthNumber(2));
        assertEquals("rd", TransformedMediaHelper.getNthNumber(3));
        assertEquals("th", TransformedMediaHelper.getNthNumber(4));
        assertEquals("th", TransformedMediaHelper.getNthNumber(18));
        assertEquals("st", TransformedMediaHelper.getNthNumber(21));
        assertEquals("nd", TransformedMediaHelper.getNthNumber(22));
        assertEquals("rd", TransformedMediaHelper.getNthNumber(23));
        assertEquals("th", TransformedMediaHelper.getNthNumber(24));
    }

    @Test
    void whenCreateTransformMediaEntityIsCalled_thenFilenameFormatSizeShouldBeSet() {

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setRequestType(AudioRequestType.PLAYBACK);
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(1);
        mediaRequest.setCreatedBy(userAccount);
        doAnswer(invocation -> invocation.getArgument(0)).when(transformedMediaRepository).save(any());
        TransformedMediaEntity transformedMediaEntity = transformedMediaHelper.createTransformedMediaEntity(
            mediaRequest,
            "case1_23_Nov_2023.mp3",
            TIME_11_59,
            TIME_12_00,
            BINARY_DATA.getLength()
        );

        assertEquals(userAccount.getId(), transformedMediaEntity.getCreatedById());
        assertEquals(userAccount.getId(), transformedMediaEntity.getLastModifiedById());
        assertEquals(TEST_FILE_NAME, transformedMediaEntity.getOutputFilename());
        assertEquals(TEST_EXTENSION, transformedMediaEntity.getOutputFormat().getExtension());
        assertEquals(TEST_BINARY_STRING.length(), transformedMediaEntity.getOutputFilesize());
    }

}
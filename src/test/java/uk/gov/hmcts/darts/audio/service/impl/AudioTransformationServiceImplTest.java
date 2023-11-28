package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

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
}

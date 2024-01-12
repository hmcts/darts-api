package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.impl.DataManagementApiImpl;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.PLAYBACK;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class MediaRequestServiceImplTest {

    private static final Integer TEST_REQUESTER = 1234;
    private static final String OFFSET_T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-31T12:00:00Z";
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");


    @InjectMocks
    private MediaRequestServiceImpl mediaRequestService;

    @Mock
    private HearingRepository mockHearingRepository;
    @Mock
    private UserAccountRepository mockUserAccountRepository;
    @Mock
    private MediaRequestRepository mockMediaRequestRepository;
    @Mock
    private TransformedMediaRepository mockTransformedMediaRepository;
    @Mock
    private TransientObjectDirectoryRepository mockTransientObjectDirectoryRepository;
    @Mock
    private DataManagementApiImpl dataManagementApi;
    @Mock
    private NotificationApi notificationApi;
    @Mock
    private AuditApi auditApi;
    @Mock
    private UserIdentity mockUserIdentity;

    private HearingEntity mockHearingEntity;
    private MediaRequestEntity mockMediaRequestEntity;
    private TransformedMediaEntity mockTransformedMediaEntity;

    @Mock
    private CourtCaseEntity mockCourtCaseEntity;
    @Mock
    private UserAccountEntity mockUserAccountEntity;

    @BeforeEach
    void beforeEach() {
        mockHearingEntity = new HearingEntity();
        mockHearingEntity.setCourtCase(mockCourtCaseEntity);

        mockMediaRequestEntity = new MediaRequestEntity();
        mockMediaRequestEntity.setHearing(mockHearingEntity);
        mockMediaRequestEntity.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        mockMediaRequestEntity.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);
        mockMediaRequestEntity.setCurrentOwner(mockUserAccountEntity);
        mockMediaRequestEntity.setStatus(OPEN);
        mockMediaRequestEntity.setAttempts(0);
        OffsetDateTime now = OffsetDateTime.now();
        mockMediaRequestEntity.setCreatedDateTime(now);
        mockMediaRequestEntity.setCreatedBy(mockUserAccountEntity);
        mockMediaRequestEntity.setLastModifiedDateTime(now);
        mockMediaRequestEntity.setLastModifiedBy(mockUserAccountEntity);

        mockTransformedMediaEntity = new TransformedMediaEntity();
        mockTransformedMediaEntity.setMediaRequest(mockMediaRequestEntity);
    }

    @Test
    void countNonAccessedAudioForUser() {
        when(mockMediaRequestRepository.countTransformedEntitiesByRequestorIdAndStatusNotAccessed(
            any(),
            eq(COMPLETED)
        )).thenReturn(10L);

        AudioNonAccessedResponse result = mediaRequestService.countNonAccessedAudioForUser(
            TEST_REQUESTER);

        assertEquals(10, result.getCount());
    }

    @Test
    void whenSavingAudioRequestIsSuccessful() {
        Integer hearingId = 4567;
        mockHearingEntity.setId(hearingId);
        mockMediaRequestEntity.setId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD);

        when(mockHearingRepository.getReferenceById(hearingId)).thenReturn(mockHearingEntity);
        when(mockMediaRequestRepository.saveAndFlush(any(MediaRequestEntity.class))).thenReturn(mockMediaRequestEntity);
        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        doNothing().when(auditApi).recordAudit(any(), any(), any());
        var request = mediaRequestService.saveAudioRequest(requestDetails);

        assertEquals(request.getId(), mockMediaRequestEntity.getId());
        verify(mockHearingRepository).getReferenceById(hearingId);
        verify(mockMediaRequestRepository).saveAndFlush(any(MediaRequestEntity.class));
        verify(mockUserAccountRepository).getReferenceById(TEST_REQUESTER);
        verify(auditApi).recordAudit(AuditActivity.REQUEST_AUDIO, mockUserAccountEntity, mockCourtCaseEntity);
    }

    @Test
    void whenNoDuplicateAudioRequestExists() {
        Integer hearingId = 4567;
        mockHearingEntity.setId(hearingId);
        mockMediaRequestEntity.setId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD);

        when(mockHearingRepository.getReferenceById(hearingId)).thenReturn(mockHearingEntity);
        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        when(mockMediaRequestRepository.findDuplicateUserMediaRequests(
            mockHearingEntity,
            mockUserAccountEntity,
            requestDetails.getStartTime(),
            requestDetails.getEndTime(),
            requestDetails.getRequestType(),
            List.of(OPEN, PROCESSING)
        )).thenReturn(Optional.empty());

        boolean isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails);

        assertFalse(isDuplicateRequest);
    }

    @Test
    void whenDuplicateAudioRequestIsFound() {
        Integer hearingId = 4567;
        mockHearingEntity.setId(hearingId);
        mockMediaRequestEntity.setId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD);

        when(mockHearingRepository.getReferenceById(hearingId)).thenReturn(mockHearingEntity);
        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        when(mockMediaRequestRepository.findDuplicateUserMediaRequests(
            mockHearingEntity,
            mockUserAccountEntity,
            requestDetails.getStartTime(),
            requestDetails.getEndTime(),
            requestDetails.getRequestType(),
            List.of(OPEN, PROCESSING)
        )).thenReturn(Optional.of(mockMediaRequestEntity));

        boolean isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails);

        assertTrue(isDuplicateRequest);
    }


    @SneakyThrows
    @Test
    @SuppressWarnings("PMD.LawOfDemeter")
    void shouldScheduleRequestPendingNotification() {
        var mockCourtCaseEntity = new CourtCaseEntity();
        mockCourtCaseEntity.setId(1001);
        mockMediaRequestEntity.getHearing().setCourtCase(mockCourtCaseEntity);
        var mockUserAccountEntity = new UserAccountEntity();
        mockUserAccountEntity.setEmailAddress("test@test.com");
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);
        mediaRequestService.scheduleMediaRequestPendingNotification(mockMediaRequestEntity);

        var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
            .eventId(NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString())
            .caseId(1001)
            .emailAddresses("test@test.com")
            .build();
        verify(notificationApi, Mockito.times(1)).scheduleNotification(eq(saveNotificationToDbRequest));
    }

    @Test
    void whenAudioRequestHasBeenProcessedDeleteBlobDataAndAudioRequest() throws AzureDeleteBlobException {
        var mediaRequestId = 1;
        UUID blobId = UUID.randomUUID();

        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(blobId);

        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(1);

        when(mockTransformedMediaRepository.findByMediaRequestId(mediaRequestId))
            .thenReturn(List.of(transformedMediaEntity));

        when(mockTransientObjectDirectoryRepository.findByTransformedMediaId(any()))
            .thenReturn(List.of(transientObjectDirectoryEntity));

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockTransformedMediaRepository).findByMediaRequestId(mediaRequestId);
        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(1)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(mockTransientObjectDirectoryRepository, Mockito.times(1)).deleteById(any());
    }

    @Test
    void whenTransientObjectHasNoExternalLocationValueAvoidDeletingFromBlobStorage() throws AzureDeleteBlobException {
        var mediaRequestId = 1;
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(null);

        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(1);

        when(mockTransformedMediaRepository.findByMediaRequestId(mediaRequestId))
            .thenReturn(List.of(transformedMediaEntity));

        when(mockTransientObjectDirectoryRepository.findByTransformedMediaId(any()))
            .thenReturn(List.of(transientObjectDirectoryEntity));


        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(0)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(mockTransientObjectDirectoryRepository, Mockito.times(1)).deleteById(any());
    }

    @Test
    void whenNoAudioIsPresentOnlyDeleteAudioRequest() throws AzureDeleteBlobException {
        var mediaRequestId = 1;
        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(1);

        when(mockTransformedMediaRepository.findByMediaRequestId(mediaRequestId))
            .thenReturn(List.of(transformedMediaEntity));

        when(mockTransientObjectDirectoryRepository.findByTransformedMediaId(any()))
            .thenReturn(new ArrayList<>());

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockTransformedMediaRepository).findByMediaRequestId(mediaRequestId);
        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(0)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(mockTransientObjectDirectoryRepository, Mockito.times(0)).deleteById(any());
    }

    @Test
    void downloadShouldReturnExpectedData() throws IOException {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(DOWNLOAD);
        mockMediaRequestEntity.setStatus(COMPLETED);

        var objectRecordStatusEntity = new ObjectRecordStatusEntity();
        objectRecordStatusEntity.setId(STORED.getId());

        var blobUuid = UUID.randomUUID();
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(blobUuid);
        transientObjectDirectoryEntity.setTransformedMedia(mockTransformedMediaEntity);

        mockTransformedMediaEntity.setTransientObjectDirectoryEntities(List.of(transientObjectDirectoryEntity));

        var transformedMediaId = 1;
        mockTransformedMediaEntity.setId(transformedMediaId);

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        when(mockUserIdentity.getUserAccount()).thenReturn(mockUserAccountEntity);
        doNothing().when(auditApi).recordAudit(any(), any(), any());

        when(dataManagementApi.getBlobDataFromOutboundContainer(blobUuid))
            .thenReturn(BinaryData.fromBytes(DUMMY_FILE_CONTENT.getBytes()));

        try (InputStream inputStream = mediaRequestService.download(transformedMediaId)) {
            byte[] bytes = inputStream.readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }

        verify(mockTransformedMediaRepository).findById(transformedMediaId);
        verifyNoInteractions(mockTransientObjectDirectoryRepository);
        verify(auditApi).recordAudit(AuditActivity.EXPORT_AUDIO, mockUserAccountEntity, mockCourtCaseEntity);
    }

    @Test
    void downloadShouldThrowExceptionWhenTransformedMediaCannotBeFound() {
        var transformedMediaId = 1;

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.download(transformedMediaId)
        );

        assertEquals(AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND, exception.getError());
    }

    @Test
    void downloadShouldThrowExceptionWhenMediaRequestTypeIsPlayback() {
        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(PLAYBACK);
        mockMediaRequestEntity.setStatus(COMPLETED);

        var transformedMediaId = 1;
        mockTransformedMediaEntity.setId(transformedMediaId);

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.download(transformedMediaId)
        );

        assertEquals(AudioRequestsApiError.MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT, exception.getError());

        verifyNoInteractions(mockMediaRequestRepository);
        verify(mockTransformedMediaRepository).findById(transformedMediaId);
    }

    @Test
    void downloadShouldThrowExceptionWhenRelatedTransientObjectCannotBeFound() {
        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(DOWNLOAD);

        var transformedMediaId = 1;
        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.download(transformedMediaId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

    @Test
    void downloadShouldThrowExceptionWhenTransientObjectHasNoExternalLocationValue() {
        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(DOWNLOAD);
        mockMediaRequestEntity.setStatus(FAILED);

        var objectRecordStatusEntity = new ObjectRecordStatusEntity();
        objectRecordStatusEntity.setId(FAILURE_CHECKSUM_FAILED.getId());

        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(null);
        mockTransformedMediaEntity.setTransientObjectDirectoryEntities(List.of(transientObjectDirectoryEntity));

        var transformedMediaId = 1;
        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.download(transformedMediaId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

    @Test
    void playbackShouldReturnExpectedData() throws IOException {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(PLAYBACK);
        mockMediaRequestEntity.setStatus(COMPLETED);

        var objectRecordStatusEntity = new ObjectRecordStatusEntity();
        objectRecordStatusEntity.setId(STORED.getId());

        var blobUuid = UUID.randomUUID();
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(blobUuid);
        transientObjectDirectoryEntity.setTransformedMedia(mockTransformedMediaEntity);

        mockTransformedMediaEntity.setTransientObjectDirectoryEntities(List.of(transientObjectDirectoryEntity));

        var transformedMediaId = 1;
        mockTransformedMediaEntity.setId(transformedMediaId);

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        when(mockUserIdentity.getUserAccount()).thenReturn(mockUserAccountEntity);
        doNothing().when(auditApi).recordAudit(any(), any(), any());

        when(dataManagementApi.getBlobDataFromOutboundContainer(blobUuid))
            .thenReturn(BinaryData.fromBytes(DUMMY_FILE_CONTENT.getBytes()));

        try (InputStream inputStream = mediaRequestService.playback(transformedMediaId)) {
            byte[] bytes = inputStream.readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }

        verify(mockTransformedMediaRepository).findById(transformedMediaId);
        verifyNoInteractions(mockTransientObjectDirectoryRepository);
        verify(auditApi).recordAudit(AuditActivity.AUDIO_PLAYBACK, mockUserAccountEntity, mockCourtCaseEntity);
    }

    @Test
    void playbackShouldThrowExceptionWhenTransformedMediaCannotBeFound() {
        var transformedMediaId = 1;

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.playback(transformedMediaId)
        );

        assertEquals(AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND, exception.getError());
    }

    @Test
    void playbackShouldThrowExceptionWhenMediaRequestTypeIsDownload() {
        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(DOWNLOAD);
        mockMediaRequestEntity.setStatus(COMPLETED);

        var transformedMediaId = 1;
        mockTransformedMediaEntity.setId(transformedMediaId);

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.playback(transformedMediaId)
        );

        assertEquals(AudioRequestsApiError.MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT, exception.getError());

        verifyNoInteractions(mockMediaRequestRepository);
        verify(mockTransformedMediaRepository).findById(transformedMediaId);
    }

    @Test
    void playbackShouldThrowExceptionWhenRelatedTransientObjectCannotBeFound() {
        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(PLAYBACK);

        var transformedMediaId = 1;
        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.playback(transformedMediaId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

    @Test
    void playbackShouldThrowExceptionWhenTransientObjectHasNoExternalLocationValue() {
        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(PLAYBACK);
        mockMediaRequestEntity.setStatus(FAILED);

        var objectRecordStatusEntity = new ObjectRecordStatusEntity();
        objectRecordStatusEntity.setId(FAILURE_CHECKSUM_FAILED.getId());

        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(null);
        mockTransformedMediaEntity.setTransientObjectDirectoryEntities(List.of(transientObjectDirectoryEntity));

        var transformedMediaId = 1;
        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.ofNullable(mockTransformedMediaEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.playback(transformedMediaId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

}

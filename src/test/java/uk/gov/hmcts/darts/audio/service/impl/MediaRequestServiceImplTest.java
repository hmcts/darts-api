package uk.gov.hmcts.darts.audio.service.impl;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.darts.audio.component.AudioRequestBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audio.mapper.GetTransformedMediaDetailsMapper;
import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.validation.AudioMediaPatchRequestValidator;
import uk.gov.hmcts.darts.audio.validation.MediaHideOrShowValidator;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchResponse;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.PLAYBACK;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.AUDIO_PLAYBACK;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.CHANGE_AUDIO_OWNERSHIP;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.EXPORT_AUDIO;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REQUEST_AUDIO;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING_ARCHIVE;
import static uk.gov.hmcts.darts.util.EntityIdPopulator.withIdsPopulatedInt;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class MediaRequestServiceImplTest {

    private static final Integer TEST_REQUESTER = 1234;
    private static final String OFFSET_T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-31T12:00:00Z";
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");

    @Mock
    private AudioMediaPatchRequestValidator mediaRequestValidator;

    @InjectMocks
    @Spy
    private MediaRequestServiceImpl mediaRequestService;

    @Mock
    private HearingsService mockHearingService;
    @Mock
    private UserAccountRepository mockUserAccountRepository;
    @Mock
    private MediaRequestRepository mockMediaRequestRepository;
    @Mock
    private TransformedMediaRepository mockTransformedMediaRepository;
    @Mock
    private TransientObjectDirectoryRepository mockTransientObjectDirectoryRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private NotificationApi notificationApi;
    @Mock
    private AuditApi auditApi;
    @Mock
    private UserIdentity mockUserIdentity;
    @Mock
    private AudioTransformationService audioTransformationService;
    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;
    @Mock
    private LogApi logApi;

    private HearingEntity mockHearingEntity;
    private MediaRequestEntity mockMediaRequestEntity;
    private TransformedMediaEntity mockTransformedMediaEntity;

    @Mock
    private CourtCaseEntity mockCourtCaseEntity;
    @Mock
    private UserAccountEntity mockUserAccountEntity;
    @Mock
    DownloadResponseMetaData responseMetaData;
    @Mock
    private AudioRequestBeingProcessedFromArchiveQuery audioRequestBeingProcessedFromArchiveQuery;

    @Mock
    private GetTransformedMediaDetailsMapper getTransformedMediaDetailsMapper;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaHideOrShowValidator mediaHideOrShowValidator;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private MediaTestData mediaTestData;

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

        mediaTestData = PersistableFactory.getMediaTestData();
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

        when(mockMediaRequestRepository.saveAndFlush(any(MediaRequestEntity.class))).thenReturn(mockMediaRequestEntity);
        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        doNothing().when(auditApi).record(any(), any(), any(CourtCaseEntity.class));
        var request = mediaRequestService.saveAudioRequest(requestDetails, DOWNLOAD, mockHearingEntity);

        assertEquals(request.getId(), mockMediaRequestEntity.getId());
        verify(mockMediaRequestRepository).saveAndFlush(any(MediaRequestEntity.class));
        verify(mockUserAccountRepository).getReferenceById(TEST_REQUESTER);
        verify(auditApi).record(REQUEST_AUDIO, mockUserAccountEntity, mockCourtCaseEntity);
    }

    @Test
    void whenNoDuplicateAudioRequestExists() {
        Integer hearingId = 4567;
        mockMediaRequestEntity.setId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));

        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        when(mockMediaRequestRepository.findDuplicateUserMediaRequests(
            mockHearingEntity,
            mockUserAccountEntity,
            requestDetails.getStartTime(),
            requestDetails.getEndTime(),
            DOWNLOAD,
            List.of(OPEN, PROCESSING)
        )).thenReturn(Optional.empty());

        boolean isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails, DOWNLOAD, mockHearingEntity);

        assertFalse(isDuplicateRequest);
    }

    @Test
    void whenDuplicateAudioRequestIsFound() {
        Integer hearingId = 4567;
        mockMediaRequestEntity.setId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));

        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        when(mockMediaRequestRepository.findDuplicateUserMediaRequests(
            mockHearingEntity,
            mockUserAccountEntity,
            requestDetails.getStartTime(),
            requestDetails.getEndTime(),
            DOWNLOAD,
            List.of(OPEN, PROCESSING)
        )).thenReturn(Optional.of(mockMediaRequestEntity));

        boolean isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails, DOWNLOAD, mockHearingEntity);

        assertTrue(isDuplicateRequest);
    }

    @Test
    void shouldScheduleNotificationWithAudioRequestBeingProcessed() {
        Integer mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        var mockCourtCaseEntity = new CourtCaseEntity();
        mockCourtCaseEntity.setId(1001);
        mockMediaRequestEntity.getHearing().setCourtCase(mockCourtCaseEntity);
        var mockUserAccountEntity = new UserAccountEntity();
        mockUserAccountEntity.setEmailAddress("test@test.com");
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);

        when(audioRequestBeingProcessedFromArchiveQuery.getResults(mediaRequestId))
            .thenReturn(emptyList());

        mediaRequestService.scheduleMediaRequestPendingNotification(mockMediaRequestEntity);

        var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
            .eventId(AUDIO_REQUEST_PROCESSING.toString())
            .caseId(1001)
            .userAccountsToEmail(List.of(mockUserAccountEntity))
            .build();
        verify(audioRequestBeingProcessedFromArchiveQuery).getResults(mediaRequestId);
        verify(notificationApi).scheduleNotification(saveNotificationToDbRequest);
    }

    @Test
    void shouldScheduleNotificationWithAudioRequestBeingProcessedFromArchive() {
        Integer mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        var mockCourtCaseEntity = new CourtCaseEntity();
        mockCourtCaseEntity.setId(1001);
        mockMediaRequestEntity.getHearing().setCourtCase(mockCourtCaseEntity);
        var mockUserAccountEntity = new UserAccountEntity();
        mockUserAccountEntity.setEmailAddress("test@test.com");
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);

        when(audioRequestBeingProcessedFromArchiveQuery.getResults(mediaRequestId))
            .thenReturn(List.of(
                new AudioRequestBeingProcessedFromArchiveQueryResult(181L),
                new AudioRequestBeingProcessedFromArchiveQueryResult(182L),
                new AudioRequestBeingProcessedFromArchiveQueryResult(183L),
                new AudioRequestBeingProcessedFromArchiveQueryResult(184L)
            ));

        mediaRequestService.scheduleMediaRequestPendingNotification(mockMediaRequestEntity);

        var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
            .eventId(AUDIO_REQUEST_PROCESSING_ARCHIVE.toString())
            .caseId(1001)
            .userAccountsToEmail(List.of(mockUserAccountEntity))
            .build();
        verify(audioRequestBeingProcessedFromArchiveQuery).getResults(mediaRequestId);
        verify(notificationApi).scheduleNotification(saveNotificationToDbRequest);
    }

    @Test
    void whenAudioRequestHasBeenProcessedDeleteBlobDataAndAudioRequest() throws AzureDeleteBlobException {
        var mediaRequestId = 1;
        String blobId = UUID.randomUUID().toString();

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
        verify(mockMediaRequestRepository).deleteById(mediaRequestId);
        verify(dataManagementApi).deleteBlobDataFromOutboundContainer(any(String.class));
        verify(mockTransientObjectDirectoryRepository).deleteById(any());
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

        verify(mockMediaRequestRepository).deleteById(mediaRequestId);
        verifyNoInteractions(dataManagementApi);
        verify(mockTransientObjectDirectoryRepository).deleteById(any());
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
        verify(mockMediaRequestRepository).deleteById(mediaRequestId);
        verify(dataManagementApi, times(0)).deleteBlobDataFromOutboundContainer(any(String.class));
        verify(mockTransientObjectDirectoryRepository, times(0)).deleteById(any());
    }

    @SneakyThrows
    @Test
    void downloadShouldReturnExpectedData() {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1L);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(DOWNLOAD);
        mockMediaRequestEntity.setStatus(COMPLETED);

        var objectRecordStatusEntity = new ObjectRecordStatusEntity();
        objectRecordStatusEntity.setId(STORED.getId());

        var blobUuid = UUID.randomUUID().toString();
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
        doNothing().when(auditApi).record(any(), any(), any(CourtCaseEntity.class));

        Resource resource = mock(Resource.class);
        when(responseMetaData.getResource()).thenReturn(resource);

        when(dataManagementApi.getBlobDataFromOutboundContainer(blobUuid)).thenReturn(responseMetaData);
        when(resource.getInputStream()).thenReturn(toInputStream(DUMMY_FILE_CONTENT, "UTF-8"));

        try (DownloadResponseMetaData downloadResponseMetaData = mediaRequestService.download(transformedMediaId)) {
            byte[] bytes = downloadResponseMetaData.getResource().getInputStream().readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }

        verify(mockTransformedMediaRepository).findById(transformedMediaId);
        verifyNoInteractions(mockTransientObjectDirectoryRepository);
        verify(auditApi).record(EXPORT_AUDIO, mockUserAccountEntity, mockCourtCaseEntity);
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

    @SneakyThrows
    @Test
    void playbackShouldReturnExpectedData() throws IOException {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1L);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        var mediaRequestId = 1;
        mockMediaRequestEntity.setId(mediaRequestId);
        mockMediaRequestEntity.setRequestType(PLAYBACK);
        mockMediaRequestEntity.setStatus(COMPLETED);

        var objectRecordStatusEntity = new ObjectRecordStatusEntity();
        objectRecordStatusEntity.setId(STORED.getId());

        var blobUuid = UUID.randomUUID().toString();
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
        doNothing().when(auditApi).record(any(), any(), any(CourtCaseEntity.class));

        when(dataManagementApi.getBlobDataFromOutboundContainer(blobUuid)).thenReturn(responseMetaData);

        Resource resource = mock(Resource.class);
        when(responseMetaData.getResource()).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(toInputStream(DUMMY_FILE_CONTENT, "UTF-8"));

        try (DownloadResponseMetaData downloadResponseMetaData = mediaRequestService.playback(transformedMediaId)) {
            byte[] bytes = downloadResponseMetaData.getResource().getInputStream().readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }

        verify(mockTransformedMediaRepository).findById(transformedMediaId);
        verifyNoInteractions(mockTransientObjectDirectoryRepository);
        verify(auditApi).record(AUDIO_PLAYBACK, mockUserAccountEntity, mockCourtCaseEntity);
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

    @Test
    void patchMediaRequestWithoutOwner() {

        UserAccountEntity requestor = new UserAccountEntity();
        requestor.setId(200);

        requestor.setId(300);

        MediaRequestEntity entity = new MediaRequestEntity();
        entity.setId(400);
        entity.setStartTime(OffsetDateTime.now());
        entity.setEndTime(OffsetDateTime.now().plusDays(2));
        entity.setCreatedDateTime(OffsetDateTime.now().minusHours(2));
        entity.setRequestor(requestor);

        Integer mediaRequestId = 100;
        MediaPatchRequest mediaPatchRequest = new MediaPatchRequest();
        UserAccountEntity owner = new UserAccountEntity();
        entity.setCurrentOwner(owner);

        when(mockMediaRequestRepository.findById(mediaRequestId)).thenReturn(Optional.ofNullable(entity));

        MediaPatchResponse expectedResponse = new MediaPatchResponse();
        when(getTransformedMediaDetailsMapper.mapToPatchResult(entity)).thenReturn(expectedResponse);

        MediaPatchResponse actualResponse = mediaRequestService.patchMediaRequest(mediaRequestId, mediaPatchRequest);

        assertEquals(expectedResponse, actualResponse);

        verify(mediaRequestValidator, times(1))
            .validate(any());
    }

    @Test
    void patchMediaRequestWithOwner() {
        Integer ownerId = 200;
        MediaPatchRequest mediaPatchRequest = new MediaPatchRequest();
        mediaPatchRequest.setOwnerId(ownerId);

        UserAccountEntity requestor = new UserAccountEntity();
        requestor.setId(300);

        requestor.setId(400);

        MediaRequestEntity entity = new MediaRequestEntity();
        entity.setId(500);
        entity.setStartTime(OffsetDateTime.now());
        entity.setEndTime(OffsetDateTime.now().plusDays(2));
        entity.setCreatedDateTime(OffsetDateTime.now().minusHours(2));
        entity.setRequestor(requestor);

        Integer mediaRequestId = 100;
        UserAccountEntity owner = new UserAccountEntity();

        entity.setCurrentOwner(owner);

        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(ownerId);

        when(mockMediaRequestRepository.findById(mediaRequestId)).thenReturn(Optional.ofNullable(entity));
        when(mockUserAccountRepository.findById(ownerId)).thenReturn(Optional.ofNullable(userAccountEntity));

        MediaPatchResponse expectedResponse = new MediaPatchResponse();
        when(getTransformedMediaDetailsMapper.mapToPatchResult(entity)).thenReturn(expectedResponse);

        MediaPatchResponse actualResponse = mediaRequestService.patchMediaRequest(mediaRequestId, mediaPatchRequest);

        assertEquals(expectedResponse, actualResponse);

        // ensure the media request entity has its owner updated to the payload owner
        assertEquals(ownerId, entity.getCurrentOwner().getId());

        // verify we have saved the record and called the validator
        verify(mediaRequestValidator, times(1))
            .validate(any());
        verify(mockMediaRequestRepository, times(1))
            .save(entity);
    }

    @Test
    @SuppressWarnings("java:S1874")
    void auditsWhenOwnerChanged() {
        var mediaRequest = withIdsPopulatedInt(PersistableFactory.getMediaRequestTestData().someMinimalRequestData());
        when(mockMediaRequestRepository.findById(any())).thenReturn(Optional.of(mediaRequest));
        when(mockUserAccountRepository.findById(any())).thenReturn(Optional.of(mediaRequest.getCurrentOwner()));

        mediaRequestService.patchMediaRequest(mediaRequest.getId(), new MediaPatchRequest().ownerId(999));

        verify(auditApi).record(CHANGE_AUDIO_OWNERSHIP);
    }

    @Test
    @SuppressWarnings("java:S1874")
    void doesNotAuditWhenOwnerNotChanged() {
        var mediaRequest = withIdsPopulatedInt(PersistableFactory.getMediaRequestTestData().someMinimalRequestData());
        when(mockMediaRequestRepository.findById(any())).thenReturn(Optional.of(mediaRequest));

        mediaRequestService.patchMediaRequest(
            mediaRequest.getId(),
            new MediaPatchRequest().ownerId(mediaRequest.getCurrentOwner().getId()));

        verifyNoInteractions(auditApi);
    }


    @Test
    void addAudioRequest_whenIsUserDuplicateAudioRequest_shouldError() {
        doReturn(true).when(mediaRequestService).isUserDuplicateAudioRequest(any(), any(), any());
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(3);
        when(mockHearingService.getHearingByIdWithValidation(3)).thenReturn(hearingEntity);

        AudioRequestDetails audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setHearingId(3);
        AudioRequestType audioRequestType = PLAYBACK;


        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaRequestService.addAudioRequest(audioRequestDetails, audioRequestType));

        assertThat(exception.getError()).isEqualTo(AudioRequestsApiError.DUPLICATE_MEDIA_REQUEST);
        verify(mediaRequestService).isUserDuplicateAudioRequest(audioRequestDetails, audioRequestType, hearingEntity);
    }

    @Test
    void addAudioRequest_handHeldCourtroomWithMoreThanMaxHandheldAudios_shouldError() {
        when(audioConfigurationProperties.getHandheldAudioCourtroomNumbers()).thenReturn(List.of("123", "321"));
        when(audioConfigurationProperties.getMaxHandheldAudioFiles()).thenReturn(3);
        doReturn(false).when(mediaRequestService).isUserDuplicateAudioRequest(any(), any(), any());
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setName("123");
        mockHearingEntity.setCourtroom(courtroomEntity);
        mockHearingEntity.setId(3);

        when(mockHearingService.getHearingByIdWithValidation(3)).thenReturn(mockHearingEntity);


        OffsetDateTime startTime = OffsetDateTime.now().minusDays(3);
        OffsetDateTime endTime = OffsetDateTime.now();

        AudioRequestDetails audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setStartTime(startTime);
        audioRequestDetails.setEndTime(endTime);
        audioRequestDetails.setHearingId(3);

        AudioRequestType audioRequestType = PLAYBACK;

        List<MediaEntity> mediaEntitiesForHearing = List.of(mock(MediaEntity.class), mock(MediaEntity.class));
        List<MediaEntity> filteredMediaEntities = List.of(mock(MediaEntity.class), mock(MediaEntity.class), mock(MediaEntity.class), mock(MediaEntity.class));

        when(audioTransformationService.getMediaByHearingId(any())).thenReturn(mediaEntitiesForHearing);
        when(audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            any(), any(), any())).thenReturn(filteredMediaEntities);


        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaRequestService.addAudioRequest(audioRequestDetails, audioRequestType));

        assertThat(exception.getError()).isEqualTo(AudioRequestsApiError.MAX_HANDHELD_AUDIO_FILES_EXCEEDED);
        assertThat(exception.getMessage()).isEqualTo(
            "Max handheld audio files exceed. The maximum supported number of handheld audio files is 3 but this request has 4"
        );


        verify(mediaRequestService).isUserDuplicateAudioRequest(audioRequestDetails, audioRequestType, mockHearingEntity);
        verify(mockHearingService).getHearingByIdWithValidation(3);
        verify(audioTransformationService).getMediaByHearingId(3);
        verify(audioTransformationService).filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntitiesForHearing, startTime, endTime);
    }

    @Test
    void addAudioRequest_handHeldCourtroomWithLessThenMaxHandheldAudios_shouldNotError() {
        when(audioConfigurationProperties.getHandheldAudioCourtroomNumbers()).thenReturn(List.of("123", "321"));
        when(audioConfigurationProperties.getMaxHandheldAudioFiles()).thenReturn(3);
        doReturn(false).when(mediaRequestService).isUserDuplicateAudioRequest(any(), any(), any());
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setName("123");
        mockHearingEntity.setCourtroom(courtroomEntity);
        mockHearingEntity.setId(3);

        when(mockHearingService.getHearingByIdWithValidation(3)).thenReturn(mockHearingEntity);


        OffsetDateTime startTime = OffsetDateTime.now().minusDays(3);
        OffsetDateTime endTime = OffsetDateTime.now();

        AudioRequestDetails audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setStartTime(startTime);
        audioRequestDetails.setEndTime(endTime);
        audioRequestDetails.setHearingId(3);

        AudioRequestType audioRequestType = PLAYBACK;

        List<MediaEntity> mediaEntitiesForHearing = List.of(mock(MediaEntity.class), mock(MediaEntity.class));
        List<MediaEntity> filteredMediaEntities = List.of(mock(MediaEntity.class), mock(MediaEntity.class), mock(MediaEntity.class));

        when(audioTransformationService.getMediaByHearingId(any())).thenReturn(mediaEntitiesForHearing);
        when(audioTransformationService.filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            any(), any(), any())).thenReturn(filteredMediaEntities);

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        doReturn(mediaRequest).when(mediaRequestService).saveAudioRequest(any(), any(), any());
        doNothing().when(mediaRequestService).scheduleMediaRequestPendingNotification(any());
        assertThat(mediaRequestService.addAudioRequest(audioRequestDetails, audioRequestType))
            .isEqualTo(mediaRequest);


        verify(mediaRequestService).isUserDuplicateAudioRequest(audioRequestDetails, audioRequestType, mockHearingEntity);
        verify(mockHearingService).getHearingByIdWithValidation(3);
        verify(audioTransformationService).getMediaByHearingId(3);
        verify(audioTransformationService).filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
            mediaEntitiesForHearing, startTime, endTime);
        verify(mediaRequestService).saveAudioRequest(audioRequestDetails, audioRequestType, mockHearingEntity);
        verify(mediaRequestService).scheduleMediaRequestPendingNotification(mediaRequest);
        verify(logApi).atsProcessingUpdate(mediaRequest);
    }

    @Test
    void addAudioRequest_nonHandHeldCourtroomWithMoreThanMaxHandheldAudios_shouldNotError() {
        when(audioConfigurationProperties.getHandheldAudioCourtroomNumbers()).thenReturn(List.of("123", "321"));
        doReturn(false).when(mediaRequestService).isUserDuplicateAudioRequest(any(), any(), any());
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setName("1234");
        mockHearingEntity.setCourtroom(courtroomEntity);
        mockHearingEntity.setId(3);

        when(mockHearingService.getHearingByIdWithValidation(3)).thenReturn(mockHearingEntity);

        OffsetDateTime startTime = OffsetDateTime.now().minusDays(3);
        OffsetDateTime endTime = OffsetDateTime.now();

        AudioRequestDetails audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setStartTime(startTime);
        audioRequestDetails.setEndTime(endTime);
        audioRequestDetails.setHearingId(3);

        AudioRequestType audioRequestType = PLAYBACK;

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        doReturn(mediaRequest).when(mediaRequestService).saveAudioRequest(any(), any(), any());
        doNothing().when(mediaRequestService).scheduleMediaRequestPendingNotification(any());

        assertThat(mediaRequestService.addAudioRequest(audioRequestDetails, audioRequestType))
            .isEqualTo(mediaRequest);

        verify(mediaRequestService).isUserDuplicateAudioRequest(audioRequestDetails, audioRequestType, mockHearingEntity);
        verify(mockHearingService).getHearingByIdWithValidation(3);

        verifyNoInteractions(audioTransformationService);
        verify(mediaRequestService).saveAudioRequest(audioRequestDetails, audioRequestType, mockHearingEntity);
        verify(mediaRequestService).scheduleMediaRequestPendingNotification(mediaRequest);
        verify(logApi).atsProcessingUpdate(mediaRequest);
    }
}
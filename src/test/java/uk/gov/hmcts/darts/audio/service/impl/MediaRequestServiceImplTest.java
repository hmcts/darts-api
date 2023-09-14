package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.impl.DataManagementApiImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.model.AudioRequestType.DOWNLOAD;

@ExtendWith(MockitoExtension.class)
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
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    @Mock
    private DataManagementApiImpl dataManagementApi;

    private HearingEntity mockHearingEntity;
    private MediaRequestEntity mockMediaRequestEntity;

    @Mock
    private UserAccountEntity mockUserAccountEntity;
    @Mock
    private AudioTransformationService audioTransformationService;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private AudioOperationService audioOperationService;
    @Mock
    private FileOperationService fileOperationService;

    @BeforeEach
    void beforeEach() {

        mockHearingEntity = new HearingEntity();

        mockMediaRequestEntity = new MediaRequestEntity();
        mockMediaRequestEntity.setHearing(mockHearingEntity);
        mockMediaRequestEntity.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        mockMediaRequestEntity.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);
        mockMediaRequestEntity.setStatus(OPEN);
        mockMediaRequestEntity.setAttempts(0);
        OffsetDateTime now = OffsetDateTime.now();
        mockMediaRequestEntity.setCreatedDateTime(now);
        mockMediaRequestEntity.setCreatedBy(mockUserAccountEntity);
        mockMediaRequestEntity.setLastModifiedDateTime(now);
        mockMediaRequestEntity.setLastModifiedBy(mockUserAccountEntity);
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
        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        assertEquals(requestId, mockMediaRequestEntity.getId());
        verify(mockHearingRepository).getReferenceById(hearingId);
        verify(mockMediaRequestRepository).saveAndFlush(any(MediaRequestEntity.class));
        verify(mockUserAccountRepository).getReferenceById(TEST_REQUESTER);
    }

    @Test
    void whenAudioRequestHasBeenProcessedDeleteBlobDataAndAudioRequest() {
        var mediaRequestId = 1;
        UUID blobId = UUID.randomUUID();

        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(blobId);

        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.of(transientObjectDirectoryEntity));

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(1)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(transientObjectDirectoryRepository, Mockito.times(1)).deleteById(any());
    }

    @Test
    void whenTransientObjectHasNoExternalLocationValueAvoidDeletingFromBlobStorage() {
        var mediaRequestId = 1;
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(null);

        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.of(transientObjectDirectoryEntity));

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(0)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(transientObjectDirectoryRepository, Mockito.times(1)).deleteById(any());
    }

    @Test
    void whenNoAudioIsPresentOnlyDeleteAudioRequest() {
        var mediaRequestId = 1;
        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.empty());

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(0)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(transientObjectDirectoryRepository, Mockito.times(0)).deleteById(any());
    }

    @Test
    void downloadShouldReturnExpectedData() throws IOException {
        var blobUuid = UUID.randomUUID();
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(blobUuid);

        var mediaRequestId = 1;
        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.of(transientObjectDirectoryEntity));
        when(audioTransformationService.getAudioBlobData(blobUuid))
            .thenReturn(BinaryData.fromBytes(DUMMY_FILE_CONTENT.getBytes()));

        try (InputStream inputStream = mediaRequestService.getProcessedAudio(mediaRequestId)) {
            byte[] bytes = inputStream.readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }
    }

    @Test
    void downloadShouldThrowExceptionWhenRelatedTransientObjectCannotBeFound() {
        var mediaRequestId = 1;
        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.getProcessedAudio(mediaRequestId)
        );

        assertEquals(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

    @Test
    void downloadShouldThrowExceptionWhenTransientObjectHasNoExternalLocationValue() {
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(null);

        var mediaRequestId = 1;
        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.of(transientObjectDirectoryEntity));

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.getProcessedAudio(mediaRequestId)
        );

        assertEquals(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

    @Test
    void previewShouldReturnExpectedData() throws IOException, ExecutionException, InterruptedException {

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        Path mediaPath = Path.of("/path/to/audio/sample2-5secs.mp2");
        when(mediaRepository.findById(1)).thenReturn(Optional.of(mediaEntity));
        when(audioTransformationService.saveMediaToWorkspace(mediaEntity)).thenReturn(mediaPath);

        AudioFileInfo audioFileInfo = new AudioFileInfo(START_TIME.toInstant(), END_TIME.toInstant(), "test", 1);
        when(audioOperationService.reEncode(anyString(), any())).thenReturn(audioFileInfo);


        byte[] testStringInBytes = DUMMY_FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        when(fileOperationService.saveFileToBinaryData(any())).thenReturn(data);

        try (InputStream inputStream = mediaRequestService.preview(mediaEntity.getId())) {
            byte[] bytes = inputStream.readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }
    }

    @Test
    void previewShouldThrowExceptionWhenMediaIdCannotBeFound() {

        var mediaRequestId = 1;

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(mediaRequestId);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        when(mediaRepository.findById(mediaRequestId)).thenReturn(Optional.empty());

        var exception = assertThrows(
            DartsApiException.class,
            () -> mediaRequestService.preview(mediaRequestId)
        );

        assertEquals(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }

}

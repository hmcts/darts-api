package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetaDataRequest;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioServiceImplTest {

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    public static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";

    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");
    @Mock
    AddAudioRequestMapper mapper;

    @Captor
    ArgumentCaptor<MediaEntity> mediaEntityArgumentCaptor;
    @Mock
    private AudioTransformationService audioTransformationService;

    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    @Mock
    private AudioOperationService audioOperationService;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    private AudioService audioService;

    @BeforeEach
    void setUp() {
        audioService = new AudioServiceImpl(
            audioTransformationService,
            transientObjectDirectoryRepository,
            mediaRepository,
            audioOperationService,
            fileOperationService,
            retrieveCoreObjectService,
            mapper
        );
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

        try (InputStream inputStream = audioService.download(mediaRequestId)) {
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
            () -> audioService.download(mediaRequestId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
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
            () -> audioService.download(mediaRequestId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
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

        try (InputStream inputStream = audioService.preview(mediaEntity.getId())) {
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
            () -> audioService.preview(mediaRequestId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }


    @Test
    void addAudio() {
        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        AddAudioMetaDataRequest addAudioRequest = createAddAudioRequest(startedAt, endedAt);
        HearingEntity hearingEntity = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any()
        )).thenReturn(hearingEntity);
        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);

        when(mapper.mapToMedia(any())).thenReturn(mediaEntity);
        audioService.addAudio(addAudioRequest);

        verify(mediaRepository).save(mediaEntityArgumentCaptor.capture());

        MediaEntity savedMedia = mediaEntityArgumentCaptor.getValue();
        assertEquals(startedAt, savedMedia.getStart());
        assertEquals(endedAt, savedMedia.getEnd());
        assertEquals(1, savedMedia.getChannel());
        assertEquals(2, savedMedia.getTotalChannels());
        assertEquals("SWANSEA", savedMedia.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals("1", savedMedia.getCourtroom().getName());

    }

    private MediaEntity createMediaEntity(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setStart(startedAt);
        mediaEntity.setEnd(endedAt);
        mediaEntity.setChannel(1);
        mediaEntity.setTotalChannels(2);
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        mediaEntity.setCourtroom(new CourtroomEntity(1, "1", courthouse));
        return mediaEntity;
    }

    private AddAudioMetaDataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        AddAudioMetaDataRequest addAudioRequest = new AddAudioMetaDataRequest();
        addAudioRequest.startedAt(startedAt);
        addAudioRequest.endedAt(endedAt);
        addAudioRequest.setChannel(1);
        addAudioRequest.totalChannels(2);
        addAudioRequest.format("mp3");
        addAudioRequest.filename("test");
        addAudioRequest.courthouse("SWANSEA");
        addAudioRequest.courtroom("1");
        addAudioRequest.cases(List.of("1", "2", "3"));
        return addAudioRequest;
    }

    @Test
    void linkAudioAndHearing() {
        AddAudioMetaDataRequest audioRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        HearingEntity hearing = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any()
        )).thenReturn(hearing);
        audioService.linkAudioAndHearing(audioRequest, mediaEntity);
        assertEquals(3, hearing.getMediaList().size());
    }
}

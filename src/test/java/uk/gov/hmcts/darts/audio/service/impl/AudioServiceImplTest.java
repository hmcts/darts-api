package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioServiceImplTest {

    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";

    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");

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

    private AudioService audioService;

    @BeforeEach
    void setUp() {
        audioService = new AudioServiceImpl(
            audioTransformationService, transientObjectDirectoryRepository,
            mediaRepository, audioOperationService, fileOperationService
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
            () -> audioService.download(mediaRequestId)
        );

        assertEquals(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }


    @Test
    void previewShouldReturnExpectedData() throws IOException, ExecutionException, InterruptedException {
        var blobUuid = UUID.randomUUID();
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(blobUuid);

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
}

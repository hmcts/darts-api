package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioServiceImplTest {

    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";

    @Mock
    private AudioTransformationService audioTransformationService;

    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    private AudioService audioService;

    @BeforeEach
    void setUp() {
        audioService = new AudioServiceImpl(audioTransformationService, transientObjectDirectoryRepository);
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

}

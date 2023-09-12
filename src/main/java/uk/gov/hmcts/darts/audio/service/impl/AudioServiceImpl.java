package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioServiceImpl implements AudioService {

    private final AudioTransformationService audioTransformationService;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final MediaRepository mediaRepository;
    private final AudioOperationService audioOperationService;
    private final FileOperationService fileOperationService;

    @Override
    public InputStream download(Integer audioRequestId) {
        var transientObjectEntity = transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(audioRequestId)
            .orElseThrow(() -> new DartsApiException(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED));

        UUID blobId = transientObjectEntity.getExternalLocation();
        if (blobId == null) {
            throw new DartsApiException(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED);
        }

        return audioTransformationService.getAudioBlobData(blobId)
            .toStream();
    }

    @Override
    public InputStream preview(Integer mediaId) {
        MediaEntity mediaEntity = mediaRepository.findById(mediaId).orElseThrow(
            () -> new DartsApiException(AudioError.REQUESTED_DATA_CANNOT_BE_LOCATED));
        BinaryData mediaBinaryData;
        try {
            Path downloadPath = audioTransformationService.saveMediaToWorkspace(mediaEntity);

            AudioFileInfo audioFileInfo = createAudioFileInfo(mediaEntity, downloadPath);

            AudioFileInfo encodedAudioFileInfo;
            try {
                encodedAudioFileInfo = audioOperationService.reEncode(UUID.randomUUID().toString(), audioFileInfo);
            } catch (ExecutionException | InterruptedException e) {
                // For Sonar rule S2142
                throw e;
            }
            Path encodedAudioPath = Path.of(encodedAudioFileInfo.getFileName());

            mediaBinaryData = fileOperationService.saveFileToBinaryData(encodedAudioPath.toFile().getAbsolutePath());
        } catch (Exception exception) {
            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return mediaBinaryData.toStream();
    }

    private static AudioFileInfo createAudioFileInfo(MediaEntity mediaEntity, Path downloadPath) {
        AudioFileInfo audioFileInfo = new AudioFileInfo(
            mediaEntity.getStart().toInstant(),
            mediaEntity.getEnd().toInstant(),
            downloadPath.toFile().getAbsolutePath(),
            mediaEntity.getChannel());
        return audioFileInfo;
    }

}

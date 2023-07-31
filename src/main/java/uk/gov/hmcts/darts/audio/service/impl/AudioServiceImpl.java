package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioServiceImpl implements AudioService {

    private final AudioTransformationService audioTransformationService;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;

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

}

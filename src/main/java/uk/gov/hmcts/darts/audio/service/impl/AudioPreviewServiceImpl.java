package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.service.AudioPreviewService;
import uk.gov.hmcts.darts.audio.validation.MediaIdValidator;

import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.ENCODING;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AudioPreviewServiceImpl implements AudioPreviewService {

    private final CachingAudioPreviewEncoder cachingAudioPreviewEncoder;
    private final MediaIdValidator mediaIdValidator;

    @Override
    public AudioPreview getOrCreateAudioPreview(Integer mediaId) {
        mediaIdValidator.validateNotHidden(mediaId);
        mediaIdValidator.validateNotZeroSecondAudio(mediaId);

        AudioPreview audioPreview = cachingAudioPreviewEncoder.getPreviewFor(mediaId);
        if (audioPreview == null) {
            audioPreview = new AudioPreview(mediaId, ENCODING, null);
            cachingAudioPreviewEncoder.cache(mediaId, audioPreview);
            log.info("Setting media with ID {} to redis with status ENCODING", mediaId);
            cachingAudioPreviewEncoder.encodeAndCachePreviewFor(mediaId);
        }
        return audioPreview;
    }

}
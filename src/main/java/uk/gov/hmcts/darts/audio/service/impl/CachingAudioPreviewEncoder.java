package uk.gov.hmcts.darts.audio.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.common.service.RedisService;

import java.util.concurrent.TimeUnit;

import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;

@Service
@Slf4j
public class CachingAudioPreviewEncoder {

    private final String folder;
    private final Long ttlInMinutes;
    private final Long failedTtlInMinutes;
    private final AudioService audioService;
    private final RedisService<AudioPreview> binaryDataRedisService;

    public CachingAudioPreviewEncoder(
        @Value("${darts.audio.preview.redis-folder}") String folder,
        @Value("${darts.audio.preview.redis-ttl-mins}") Long ttlInMinutes,
        @Value("${darts.audio.preview.redis-failed-ttl-mins}") Long failedTtlInMinutes,
        AudioService audioService,
        RedisService<AudioPreview> binaryDataRedisService) {

        this.folder = folder;
        this.ttlInMinutes = ttlInMinutes;
        this.failedTtlInMinutes = failedTtlInMinutes;
        this.audioService = audioService;
        this.binaryDataRedisService = binaryDataRedisService;
    }

    @Async
    public void encodeAndCachePreviewFor(Long mediaId) {
        AudioPreview audioPreview = getPreviewFor(mediaId);
        try {
            log.info("Encoding media with ID {}", mediaId);
            byte[] audio = audioService.encode(mediaId).toBytes();
            log.info("Encoded media with ID {}, setting as READY", mediaId);
            audioPreview.setAudio(audio);
            audioPreview.setStatus(READY);
        } catch (RuntimeException exception) {
            log.error("Failed to encode media with ID {}", mediaId, exception);
            log.info("Encoded media with ID {}, setting as FAILED", mediaId);
            audioPreview.setStatus(FAILED);
        }
        cache(mediaId, audioPreview);
    }

    public void cache(Long key, AudioPreview audioPreview) {
        binaryDataRedisService.writeToRedis(folder, key.toString(), audioPreview);
        Long ttl = ttlInMinutes;
        if (audioPreview.getStatus().equals(FAILED)) {
            ttl = failedTtlInMinutes;
        }
        binaryDataRedisService.setTtl(folder, key.toString(), ttl, TimeUnit.MINUTES);
    }

    public AudioPreview getPreviewFor(Long key) {
        return binaryDataRedisService.readFromRedis(folder,  key.toString());
    }

}
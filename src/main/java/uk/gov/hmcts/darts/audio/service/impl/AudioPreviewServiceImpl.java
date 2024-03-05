package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.service.AudioPreviewService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.common.service.RedisService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AudioPreviewServiceImpl implements AudioPreviewService {

    @Value("${darts.audio.preview.redis-folder}")
    private String folder;
    @Value("${darts.audio.preview.redis-ttl-mins}")
    private Integer ttlInMinutes;

    @Value("${darts.audio.preview.redis-failed-ttl-mins}")
    private Integer failedTtlInMinutes;

    private final AudioService audioService;
    private final RedisService<AudioPreview> binaryDataRedisService;

    @Override
    public AudioPreview getOrCreateAudioPreview(Integer mediaId) {
        String key = mediaId.toString();
        AudioPreview audioPreview = get(key);
        if (audioPreview == null) {
            audioPreview = new AudioPreview(mediaId, AudioPreviewStatus.ENCODING.toString(), null);
            save(key, audioPreview);
            log.info("Setting media with ID {} to redis with status ENCODING", mediaId);
            CompletableFuture.runAsync(() -> encodeAndSaveAudioPreview(mediaId));
        }
        return audioPreview;
    }

    private void save(String key, AudioPreview audioPreview) {
        binaryDataRedisService.writeToRedis(folder, key, audioPreview);
        Integer ttl = ttlInMinutes;
        if (audioPreview.getStatus().equals(AudioPreviewStatus.FAILED.toString())) {
            ttl = failedTtlInMinutes;
        }
        binaryDataRedisService.setTTL(folder, key, ttl, TimeUnit.MINUTES);
    }

    private AudioPreview get(String key) {
        return binaryDataRedisService.readFromRedis(folder, key);
    }

    private void delete(String key) {
        binaryDataRedisService.deleteFromRedis(folder, key);
    }

    @Async
    private void encodeAndSaveAudioPreview(Integer mediaId) {
        String key = mediaId.toString();
        AudioPreview audioPreview = get(key);
        try {
            log.info("Encoding media with ID {}", mediaId);
            byte[] audio = audioService.encode(mediaId).toBytes();
            log.info("Encoded media with ID {}, setting as READY", mediaId);
            audioPreview.setAudio(audio);
            audioPreview.setStatus(AudioPreviewStatus.READY.toString());
        } catch (Exception exception) {
            log.error("Failed to encode media with ID {}", mediaId, exception);
            log.info("Encoded media with ID {}, setting as FAILED", mediaId);
            audioPreview.setStatus(AudioPreviewStatus.FAILED.toString());
        } finally {
            save(key, audioPreview);
        }
    }

}
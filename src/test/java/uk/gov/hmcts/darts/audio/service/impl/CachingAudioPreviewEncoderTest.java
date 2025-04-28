package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.common.service.RedisService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;

@ExtendWith(MockitoExtension.class)
class CachingAudioPreviewEncoderTest {

    private static final String FOLDER = "some-folder";
    private static final Long TTL_IN_MINUTES = 1L;
    private static final Long FAILED_TTL_IN_MINUTES = -1L;

    @Mock
    private AudioService audioService;
    @Mock
    private RedisService<AudioPreview> binaryDataRedisService;

    private CachingAudioPreviewEncoder cachingAudioPreviewEncoder;

    private final ArgumentCaptor<AudioPreview> previewCaptor = ArgumentCaptor.forClass(AudioPreview.class);

    @BeforeEach
    void setUp() {
        cachingAudioPreviewEncoder = new CachingAudioPreviewEncoder(
            FOLDER,
            TTL_IN_MINUTES,
            FAILED_TTL_IN_MINUTES,
            audioService,
            binaryDataRedisService);
    }

    @Test
    void cachesPreviewOnSuccessfulEncoding() {
        AudioPreview cachedAudioPreview = someAudioPreview();
        when(binaryDataRedisService.readFromRedis(FOLDER, "1")).thenReturn(cachedAudioPreview);
        when(audioService.encode(1L)).thenReturn(someBinaryData());

        cachingAudioPreviewEncoder.encodeAndCachePreviewFor(1L);

        verify(binaryDataRedisService, times(1))
            .writeToRedis(eq(FOLDER), eq("1"), previewCaptor.capture());
        verify(binaryDataRedisService, times(1))
            .setTtl(eq(FOLDER), eq("1"), eq(TTL_IN_MINUTES), eq(TimeUnit.MINUTES));

        assertThat(previewCaptor.getValue())
            .hasFieldOrPropertyWithValue("status", READY);
    }

    @Test
    void cachesPreviewOnFailedEncoding() {
        AudioPreview cachedAudioPreview = someAudioPreview();
        when(binaryDataRedisService.readFromRedis(FOLDER, "1")).thenReturn(cachedAudioPreview);
        when(audioService.encode(1L)).thenThrow(new RuntimeException());

        cachingAudioPreviewEncoder.encodeAndCachePreviewFor(1L);

        verify(binaryDataRedisService, times(1))
            .writeToRedis(eq(FOLDER), eq("1"), previewCaptor.capture());
        verify(binaryDataRedisService, times(1))
            .setTtl(eq(FOLDER), eq("1"), eq(FAILED_TTL_IN_MINUTES), eq(TimeUnit.MINUTES));

        assertThat(previewCaptor.getValue())
            .hasFieldOrPropertyWithValue("status", FAILED);
    }

    private BinaryData someBinaryData() {
        return BinaryData.fromBytes("some-data".getBytes());
    }

    private AudioPreview someAudioPreview() {
        return new AudioPreview();
    }
}
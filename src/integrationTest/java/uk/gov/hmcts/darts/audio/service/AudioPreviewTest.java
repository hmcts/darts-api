package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.service.RedisService;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.ENCODING;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;
import static uk.gov.hmcts.darts.test.common.AwaitabilityUtil.waitForMax10SecondsWithOneSecondPoll;

@TestPropertySource(properties = {"darts.audio.transformation.service.audio.file=tests/audio/WithViqHeader/viq0001min.mp2"})
class AudioPreviewTest extends IntegrationBase {

    public static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);

    @Value("${darts.audio.preview.redis-folder}")
    private String folder;

    @Autowired
    private AudioPreviewService audioPreviewService;

    @Autowired
    private RedisService<AudioPreview> binaryDataRedisService;

    private HearingEntity hearing;
    private MediaEntity mediaEntity;

    @BeforeEach
    void setUp() {
        hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
    }

    @AfterEach
    void cleanup() {
        if (nonNull(mediaEntity)) {
            binaryDataRedisService.deleteFromRedis(folder, mediaEntity.getId().toString());
        }
    }

    @Test
    void generatesAndCachesAudioPreviewOnCacheMiss() {
        mediaEntity = givenSomeStoredMedia();

        var audioPreview = audioPreviewService.getOrCreateAudioPreview(mediaEntity.getId());

        assertThat(audioPreview.getStatus()).isEqualTo(ENCODING).isNotNull();
        assertThat(binaryDataRedisService.readFromRedis(folder, mediaEntity.getId().toString()))
            .hasFieldOrPropertyWithValue("status", ENCODING);
    }

    @Test
    void audioPreviewEventuallyBecomesReady() {
        mediaEntity = givenSomeStoredMedia();

        var audioPreview = audioPreviewService.getOrCreateAudioPreview(mediaEntity.getId());

        assertThat(audioPreview.getStatus()).isEqualTo(ENCODING).isNotNull();
        waitForMax10SecondsWithOneSecondPoll(() -> {
            var cachedAudioPreview = binaryDataRedisService.readFromRedis(folder, mediaEntity.getId().toString());
            return cachedAudioPreview.getStatus().equals(READY);
        });
    }

    private MediaEntity givenSomeStoredMedia() {
        var mediaEntity = PersistableFactory.getMediaTestData().someMinimalMedia();
        hearing.addMedia(mediaEntity);
        dartsPersistence.save(hearing);

        var externalObjectDirectory = PersistableFactory.getExternalObjectDirectoryTestData().minimalExternalObjectDirectory();
        externalObjectDirectory.setMedia(mediaEntity);
        dartsPersistence.save(externalObjectDirectory);
        return mediaEntity;
    }
}
package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.builder.TestMediaEntity;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;

public final class MediaTestData implements Persistable<TestMediaEntity.TestMediaBuilderRetrieve,
    MediaEntity,
    TestMediaEntity.TestMediaEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    public static final byte[] MEDIA_TEST_DATA_BINARY_DATA = "test binary data".getBytes();

    @Override
    public MediaEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestMediaEntity.TestMediaBuilderRetrieve someMinimalBuilderHolder() {
        var builderRetrieve = new TestMediaEntity.TestMediaBuilderRetrieve();

        builderRetrieve.getBuilder()
            .courtroom(PersistableFactory.getCourtroomTestData().someMinimal())
            .channel(1)
            .totalChannels(1)
            .start(NOW)
            .end(NOW.plusMinutes(1))
            .createdDateTime(NOW)
            .createdById(0)
            .lastModifiedDateTime(NOW)
            .lastModifiedById(0)
            .mediaFile("a-media-file")
            .mediaFormat("mp2")
            .fileSize(1000L)
            .mediaType(MEDIA_TYPE_DEFAULT)
            .isHidden(false)
            .isDeleted(false);

        return builderRetrieve;
    }

    @Override
    public TestMediaEntity.TestMediaEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    /**
     * Create a "minimal" media entity.
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    public MediaEntity someMinimalMedia() {
        var media = new MediaEntity();
        media.setChannel(1);
        media.setTotalChannels(1);
        media.setStart(now());
        media.setEnd(now().plusMinutes(1));
        media.setMediaFile("a-media-file");
        media.setFileSize(1000L);
        media.setMediaFormat("mp2");
        media.setMediaType(MEDIA_TYPE_DEFAULT);
        media.setCourtroom(someMinimalCourtRoom());
        media.setIsCurrent(true);

        media.setCreatedById(0);
        media.setLastModifiedById(0);

        return media;
    }

    /**
     * Create a media with specified properties.
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    public MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaWith(courtroomEntity, startTime, endTime, channel, "mp2", null, "reason");
    }

    /**
     * Create a media with specified properties.
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    public MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                       String mediaType, RetentionConfidenceScoreEnum retConfScore, String retConfReason) {
        var mediaEntity = someMinimalMedia();
        mediaEntity.setCourtroom(courtroomEntity);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        mediaEntity.setTotalChannels(2);
        mediaEntity.setMediaFormat(mediaType);
        mediaEntity.setChecksum(getChecksum());
        mediaEntity.setRetConfScore(retConfScore);
        mediaEntity.setRetConfReason(retConfReason);
        return mediaEntity;
    }

    /**
     * Create a media with specified properties.
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    public MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                       String mediaType) {
        var mediaEntity = someMinimalMedia();
        mediaEntity.setCourtroom(courtroomEntity);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        mediaEntity.setTotalChannels(2);
        mediaEntity.setMediaFormat(mediaType);
        mediaEntity.setChecksum(getChecksum());

        return mediaEntity;
    }

    /**
     * Get the checksum of the test data.
     * @deprecated As all usages are limited to other deprecated methods.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    private String getChecksum() {
        return TestUtils.encodeToString(md5(MEDIA_TEST_DATA_BINARY_DATA));
    }

}
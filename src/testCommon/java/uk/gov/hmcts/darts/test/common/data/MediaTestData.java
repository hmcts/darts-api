package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.builder.TestMediaEntity;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class MediaTestData implements Persistable<TestMediaEntity.TestMediaBuilderRetrieve,
    MediaEntity,
    TestMediaEntity.TestMediaEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    public static final byte[] MEDIA_TEST_DATA_BINARY_DATA = "test binary data".getBytes();

    /**
     * Deprecated.
     *
     * @deprecated do not use. Instead, use Persistable to create an object with the desired state.
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

        var userAccount = minimalUserAccount();
        media.setCreatedBy(userAccount);
        media.setLastModifiedBy(userAccount);

        return media;
    }

    @Deprecated
    public MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaWith(courtroomEntity, startTime, endTime, channel, "mp2", null, "reason");
    }

    @Deprecated
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

    @Deprecated
    private String getChecksum() {
        return TestUtils.encodeToString(md5(MEDIA_TEST_DATA_BINARY_DATA));
    }

    @Override
    public MediaEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestMediaEntity.TestMediaBuilderRetrieve someMinimalBuilderHolder() {
        var builderRetrieve = new TestMediaEntity.TestMediaBuilderRetrieve();

        UserAccountEntity someUser = PersistableFactory.getUserAccountTestData().someMinimal();

        builderRetrieve.getBuilder()
            .courtroom(PersistableFactory.getCourtroomTestData().someMinimal())
            .channel(1)
            .totalChannels(1)
            .start(NOW)
            .end(NOW.plusMinutes(1))
            .createdDateTime(NOW)
            .createdBy(someUser)
            .lastModifiedDateTime(NOW)
            .lastModifiedBy(someUser)
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

}
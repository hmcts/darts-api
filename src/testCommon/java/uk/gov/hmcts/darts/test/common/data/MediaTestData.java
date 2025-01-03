package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.builder.TestMediaEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class MediaTestData implements Persistable<TestMediaEntity.TestMediaBuilderRetrieve,
    MediaEntity,
    TestMediaEntity.TestMediaEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime YESTERDAY = NOW.minusDays(1);

    public static final byte[] MEDIA_TEST_DATA_BINARY_DATA = "test binary data".getBytes();

    private OffsetDateTime createdAt = NOW;

    private OffsetDateTime lastModifiedAt = NOW;

    private CourtroomEntity courtroomTestData = CourtroomTestData.someMinimalCourtRoom();

    MediaTestData() {

    }

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

    public MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaWith(courtroomEntity, startTime, endTime, channel, "mp2", 100, "reason");
    }

    public MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                       String mediaType, Integer refConfScore, String reFConfReason) {
        var mediaEntity = someMinimalMedia();
        mediaEntity.setCourtroom(courtroomEntity);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        mediaEntity.setTotalChannels(2);
        mediaEntity.setMediaFormat(mediaType);
        mediaEntity.setChecksum(getChecksum());
        mediaEntity.setRetConfScore(refConfScore);
        mediaEntity.setRetConfReason(reFConfReason);
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

    private String getChecksum() {
        return TestUtils.encodeToString(md5(MEDIA_TEST_DATA_BINARY_DATA));
    }

    public MediaEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestMediaEntity.TestMediaBuilderRetrieve someMinimalBuilderHolder() {
        var userAccount = minimalUserAccount();
        TestMediaEntity.TestMediaBuilderRetrieve builder = new TestMediaEntity.TestMediaBuilderRetrieve();
        builder.getBuilder().channel(1).totalChannels(1).start(now())
            .end(now()).mediaFile("a-media-file")
            .fileSize(1000L).mediaFormat("mp2").fileSize(1000L)
            .mediaType(MEDIA_TYPE_DEFAULT).courtroom(someMinimalCourtRoom())
            .isCurrent(true).lastModifiedBy(userAccount).deletedBy(userAccount)
            .createdBy(userAccount).lastModifiedDateTime(NOW)
            .createdDateTime(NOW).courtroom(courtroomTestData)
            .createdDateTime(createdAt)
            .lastModifiedDateTime(lastModifiedAt)
            .hearingList(new ArrayList<>());
        return builder;
    }

    @Override
    public TestMediaEntity.TestMediaEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}
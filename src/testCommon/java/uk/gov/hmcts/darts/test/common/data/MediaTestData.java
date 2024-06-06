package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class MediaTestData {

    public static final byte[] MEDIA_TEST_DATA_BINARY_DATA = "test binary data".getBytes();

    public static MediaEntity someMinimalMedia() {
        var media = new MediaEntity();
        media.setChannel(1);
        media.setTotalChannels(1);
        media.setStart(now());
        media.setEnd(now());
        media.setMediaFile("a-media-file");
        media.setFileSize(1000L);
        media.setMediaFormat("mp2");
        media.setMediaType(MEDIA_TYPE_DEFAULT);
        return media;
    }

    public static MediaEntity createMediaFor(CourtroomEntity courtroomEntity) {
        MediaEntity media = new MediaEntity();
        media.setChannel(1);
        media.setTotalChannels(2);
        media.setStart(now());
        media.setEnd(now());
        media.setCourtroom(courtroomEntity);
        media.setMediaFile("a-media-file");
        media.setChecksum(getChecksum());
        media.setFileSize(1000L);
        media.setMediaFormat("mp2");
        media.setMediaType(MEDIA_TYPE_DEFAULT);
        return media;
    }

    public static MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaWith(courtroomEntity, startTime, endTime, channel, "mp2");
    }

    public static MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                              String mediaType) {
        var mediaEntity = someMinimalMedia();
        mediaEntity.setCourtroom(courtroomEntity);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        mediaEntity.setTotalChannels(2);
        mediaEntity.setMediaFormat(mediaType);
        mediaEntity.setMediaFile("a-media-file");
        mediaEntity.setFileSize(1000L);
        mediaEntity.setChecksum(getChecksum());
        mediaEntity.setMediaType(MEDIA_TYPE_DEFAULT);

        return mediaEntity;
    }

    private String getChecksum() {
        return TestUtils.encodeToString(md5(MEDIA_TEST_DATA_BINARY_DATA));
    }

}
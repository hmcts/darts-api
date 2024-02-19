package uk.gov.hmcts.darts.testutils.data;

import com.azure.core.util.BinaryData;
import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.OffsetDateTime;

import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class MediaTestData {

    private static final String TEST_BINARY_DATA = "test binary data";

    public static BinaryData getBinaryData() {
        return BinaryData.fromString(TEST_BINARY_DATA);
    }

    public static MediaEntity someMinimalMedia() {
        return new MediaEntity();
    }

    public static MediaEntity createMediaFor(CourtroomEntity courtroomEntity) {
        MediaEntity media = new MediaEntity();
        media.setChannel(1);
        media.setTotalChannels(2);
        media.setStart(OffsetDateTime.now());
        media.setEnd(OffsetDateTime.now());
        media.setCourtroom(courtroomEntity);
        media.setMediaFile("a-media-file");
        media.setChecksum(getChecksum());
        media.setFileSize(1000L);
        media.setMediaFormat("mp2");
        media.setMediaType(MEDIA_TYPE_DEFAULT);
        return media;
    }

    public static MediaEntity createMediaWith(CourtroomEntity courtroomEntity, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        var mediaEntity = someMinimalMedia();
        mediaEntity.setCourtroom(courtroomEntity);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        mediaEntity.setTotalChannels(2);
        mediaEntity.setMediaFormat("mp2");
        mediaEntity.setMediaFile("a-media-file");
        mediaEntity.setFileSize(1000L);
        mediaEntity.setChecksum(getChecksum());
        mediaEntity.setMediaType(MEDIA_TYPE_DEFAULT);
        return mediaEntity;
    }

    private String getChecksum() {
        return new String(encodeBase64(md5(getBinaryData().toBytes())));
    }

}

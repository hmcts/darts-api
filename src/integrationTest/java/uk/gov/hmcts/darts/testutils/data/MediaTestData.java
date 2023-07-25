package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.OffsetDateTime;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class MediaTestData {

    public static MediaEntity someMinimalMedia() {
        return new MediaEntity();
    }

    public static MediaEntity createMediaFor(CourtroomEntity courtroomEntity) {
        MediaEntity media = new MediaEntity();
        media.setCourtroom(courtroomEntity);
        return media;
    }

    public static MediaEntity createMediaWith(OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        var mediaEntity = someMinimalMedia();
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        return mediaEntity;
    }
}

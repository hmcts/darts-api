package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingMediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class HearingMediaTestData {

    public static MediaEntity createMedia(CourtroomEntity courtroomEntity) {
        MediaEntity media = new MediaEntity();
        media.setCourtroom(courtroomEntity);
        return media;
    }

    public static HearingMediaEntity createHearingMedia(HearingEntity hearingEntity, MediaEntity mediaEntity) {
        var hearingMediaEntity = new HearingMediaEntity();
        hearingMediaEntity.setHearing(hearingEntity);
        hearingMediaEntity.setMedia(mediaEntity);

        return hearingMediaEntity;
    }
}

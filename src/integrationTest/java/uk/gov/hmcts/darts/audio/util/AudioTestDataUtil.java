package uk.gov.hmcts.darts.audio.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.model.AudioRequestType.DOWNLOAD;

@UtilityClass
public class AudioTestDataUtil {

    public MediaRequestEntity createMediaRequest(HearingEntity hearingEntity, Integer requestor,
                                                 OffsetDateTime startTime, OffsetDateTime endTime) {
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setOutputFormat(null);
        mediaRequestEntity.setOutputFilename(null);
        mediaRequestEntity.setLastAccessedDateTime(null);
        return mediaRequestEntity;
    }

}

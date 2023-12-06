package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;

@UtilityClass
public class AudioTestData {

    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime, OffsetDateTime lastAccessedTime,
                                                        AudioRequestType audioRequestType, AudioRequestStatus status) {
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(requestor);
        mediaRequestEntity.setStatus(status);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);
        return mediaRequestEntity;
    }

    public MediaRequestEntity createExpiredMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime,
                                                        AudioRequestType audioRequestType) {

        OffsetDateTime now = OffsetDateTime.now(UTC);
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(requestor);
        mediaRequestEntity.setStatus(AudioRequestStatus.EXPIRED);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);
        return mediaRequestEntity;
    }

    public MediaRequestEntity createCompletedMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                          OffsetDateTime startTime, OffsetDateTime endTime,
                                                          AudioRequestType audioRequestType) {
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(requestor);
        mediaRequestEntity.setStatus(AudioRequestStatus.COMPLETED);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);
        return mediaRequestEntity;
    }

}

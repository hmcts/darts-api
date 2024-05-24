package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@UtilityClass
public class AudioTestData {

    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime,
                                                        AudioRequestType audioRequestType, MediaRequestStatus status) {

        return createCurrentMediaRequest(hearingEntity, requestor, requestor, startTime, endTime, audioRequestType, status, OffsetDateTime.now());
    }

    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime,
                                                        AudioRequestType audioRequestType, MediaRequestStatus status, OffsetDateTime requestedDate) {
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(owner);
        mediaRequestEntity.setStatus(status);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);
        mediaRequestEntity.setCreatedDateTime(requestedDate);

        return mediaRequestEntity;
    }

    public MediaRequestEntity createExpiredMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime,
                                                        AudioRequestType audioRequestType) {

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(requestor);
        mediaRequestEntity.setStatus(MediaRequestStatus.EXPIRED);
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
        mediaRequestEntity.setStatus(MediaRequestStatus.COMPLETED);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);
        return mediaRequestEntity;
    }

}
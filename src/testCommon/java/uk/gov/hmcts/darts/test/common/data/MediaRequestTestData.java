package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.someMinimalHearing;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class MediaRequestTestData {

    public static MediaRequestEntity someMinimalRequestData() {
        var mediaRequest = new MediaRequestEntity();
        mediaRequest.setHearing(someMinimalHearing());
        mediaRequest.setStatus(OPEN);
        mediaRequest.setRequestType(DOWNLOAD);
        mediaRequest.setStartTime(middayToday());
        mediaRequest.setEndTime(middayToday().plusHours(1));
        var userAccount = minimalUserAccount();
        mediaRequest.setRequestor(userAccount);
        mediaRequest.setCurrentOwner(userAccount);
        mediaRequest.setCreatedBy(userAccount);
        mediaRequest.setLastModifiedBy(userAccount);
        return mediaRequest;
    }

    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime,
                                                        AudioRequestType audioRequestType, MediaRequestStatus status) {

        return createCurrentMediaRequest(hearingEntity, requestor, requestor, startTime, endTime, audioRequestType, status, OffsetDateTime.now());
    }

    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
                                                        OffsetDateTime startTime, OffsetDateTime endTime,
                                                        AudioRequestType audioRequestType, MediaRequestStatus status, OffsetDateTime requestedDate) {
        MediaRequestEntity mediaRequestEntity = someMinimalRequestData();
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

    private static OffsetDateTime middayToday() {
        return OffsetDateTime.of(LocalDate.now(), LocalTime.of(12, 0), UTC);
    }
}

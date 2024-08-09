package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.someMinimalHearing;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.getSystemUser;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class MediaRequestTestData {

    public static MediaRequestEntity minimalRequestData() {
        var mediaRequest = new MediaRequestEntity();
        mediaRequest.setHearing(someMinimalHearing());
        mediaRequest.setRequestor(minimalUserAccount());
        mediaRequest.setStatus(OPEN);
        mediaRequest.setRequestType(DOWNLOAD);
        mediaRequest.setStartTime(middayToday());
        mediaRequest.setEndTime(middayToday().plusHours(1));
        mediaRequest.setCurrentOwner(minimalUserAccount());
        mediaRequest.setCreatedBy(getSystemUser());
        mediaRequest.setLastModifiedBy(getSystemUser());
        return mediaRequest;
    }

    private static OffsetDateTime middayToday() {
        return OffsetDateTime.of(LocalDate.now(), LocalTime.of(12, 0), UTC);
    }
}

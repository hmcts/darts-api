package uk.gov.hmcts.darts.test.common.data;

import lombok.Builder;
import lombok.NonNull;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomMediaRequestEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

@SuppressWarnings("SummaryJavadoc")
public class MediaRequestTestData implements Persistable<CustomMediaRequestEntity.CustomMediaRequestEntityBuilder> {

    public CustomMediaRequestEntity.CustomMediaRequestEntityBuilder someMinimal() {
        return CustomMediaRequestEntity.builder()
                            .status(MediaRequestStatus.PROCESSING)
                            .requestType(AudioRequestType.PLAYBACK);
    }

    public CustomMediaRequestEntity.CustomMediaRequestEntityBuilder someMaximal() {
        return someMinimalRequestData();
    }

    /**
     * @deprecated do not use. Instead, use someMinimal().
     */
    @Deprecated
    public static CustomMediaRequestEntity.CustomMediaRequestEntityBuilder someMinimalRequestData() {
        return new MediaRequestTestData().someMinimal();
    }

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status) {
        return someMinimalRequestData()
                   .hearing(hearingEntity)
                   .requestor(requestor)
                   .currentOwner(requestor)
                   .startTime(startTime)
                   .endTime(endTime)
                   .requestType(audioRequestType)
                   .status(status)
                   .build();
}

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status, OffsetDateTime requestedDate) {
        return CustomMediaRequestEntity.builder()
                   .hearing(hearingEntity)
                   .requestor(requestor)
                   .currentOwner(owner)
                   .startTime(startTime)
                   .endTime(endTime)
                   .requestType(audioRequestType)
                   .status(status)
                   .createdBy(requestor)
                   .createdAt(requestedDate)
                   .lastModifiedBy(requestor)
                   .build();
    }

}
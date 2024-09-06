package uk.gov.hmcts.darts.test.common.data;

import lombok.Builder;
import lombok.NonNull;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

@SuppressWarnings("SummaryJavadoc")
public class MediaRequestTestData implements Persistable<MediaRequestEntity, MediaRequestTestData.TestSpec> {

    @Builder
    public static class TestSpec {

        private static final OffsetDateTime NOW = OffsetDateTime.now();
        private static final OffsetDateTime YESTERDAY = NOW.minusDays(1);

        private Integer id;
        @NonNull
        @Builder.Default
        private HearingEntity hearing = HearingTestData.someMinimalHearing();
        @NonNull
        @Builder.Default
        private UserAccountEntity currentOwner = UserAccountTestData.minimalUserAccount();
        @NonNull
        @Builder.Default
        private UserAccountEntity requestor = UserAccountTestData.minimalUserAccount();
        @NonNull
        @Builder.Default
        private MediaRequestStatus status = OPEN;
        @NonNull
        @Builder.Default
        private AudioRequestType requestType = DOWNLOAD;
        @NonNull
        @Builder.Default
        private Integer attempts = 0;
        @NonNull
        @Builder.Default
        private OffsetDateTime startTime = YESTERDAY;
        @NonNull
        @Builder.Default
        private OffsetDateTime endTime = YESTERDAY.plusHours(1);
        @NonNull
        @Builder.Default
        private UserAccountEntity createdBy = UserAccountTestData.minimalUserAccount();
        @NonNull
        @Builder.Default
        private UserAccountEntity lastModifiedBy = UserAccountTestData.minimalUserAccount();
        @NonNull
        @Builder.Default
        private OffsetDateTime createdAt = NOW;
        @NonNull
        @Builder.Default
        private OffsetDateTime lastModifiedAt = NOW;
    }

    public MediaRequestEntity fromSpec(TestSpec testSpec) {
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setId(testSpec.id);
        mediaRequestEntity.setHearing(testSpec.hearing);
        mediaRequestEntity.setCurrentOwner(testSpec.currentOwner);
        mediaRequestEntity.setRequestor(testSpec.requestor);
        mediaRequestEntity.setStatus(testSpec.status);
        mediaRequestEntity.setRequestType(testSpec.requestType);
        mediaRequestEntity.setAttempts(testSpec.attempts);
        mediaRequestEntity.setStartTime(testSpec.startTime);
        mediaRequestEntity.setEndTime(testSpec.endTime);
        mediaRequestEntity.setCreatedBy(testSpec.createdBy);
        mediaRequestEntity.setCreatedDateTime(testSpec.createdAt);
        mediaRequestEntity.setLastModifiedBy(testSpec.lastModifiedBy);
        mediaRequestEntity.setLastModifiedDateTime(testSpec.lastModifiedAt);

        return mediaRequestEntity;
    }

    public MediaRequestEntity someMinimal() {
        return fromSpec(TestSpec.builder()
                            .status(MediaRequestStatus.PROCESSING)
                            .requestType(AudioRequestType.PLAYBACK)
                            .build());
    }

    public MediaRequestEntity someMaximal() {
        return someMinimalRequestData();
    }

    /**
     * @deprecated do not use. Instead, use someMinimal().
     */
    @Deprecated
    public static MediaRequestEntity someMinimalRequestData() {
        return new MediaRequestTestData().someMinimal();
    }

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status) {
        return new MediaRequestTestData().fromSpec(TestSpec.builder()
                                                       .hearing(hearingEntity)
                                                       .requestor(requestor)
                                                       .currentOwner(requestor)
                                                       .startTime(startTime)
                                                       .endTime(endTime)
                                                       .requestType(audioRequestType)
                                                       .status(status)
                                                       .build());
    }

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status, OffsetDateTime requestedDate) {
        return new MediaRequestTestData().fromSpec(TestSpec.builder()
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
                                                       .build());
    }

}

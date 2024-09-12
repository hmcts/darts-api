package uk.gov.hmcts.darts.test.common.data;

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
public class MediaRequestTestData implements Persistable<CustomMediaRequestEntity.CustomMediaBuilderRetrieve> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime YESTERDAY = NOW.minusDays(1);

    private Integer id;

    private HearingEntity hearing = HearingTestData.someMinimalHearing();

    private UserAccountEntity currentOwner = UserAccountTestData.minimalUserAccount();

    private UserAccountEntity requestor = UserAccountTestData.minimalUserAccount();

    private MediaRequestStatus status = OPEN;

    private AudioRequestType requestType = DOWNLOAD;

    private Integer attempts = 0;

    private OffsetDateTime startTime = YESTERDAY;

    private OffsetDateTime endTime = YESTERDAY.plusHours(1);

    private UserAccountEntity createdBy = UserAccountTestData.minimalUserAccount();

    private UserAccountEntity lastModifiedBy = UserAccountTestData.minimalUserAccount();

    private OffsetDateTime createdAt = NOW;

    private OffsetDateTime lastModifiedAt = NOW;

    MediaRequestTestData() {

    }

    public CustomMediaRequestEntity.CustomMediaBuilderRetrieve someMinimal() {
        CustomMediaRequestEntity.CustomMediaBuilderRetrieve builder = new CustomMediaRequestEntity.CustomMediaBuilderRetrieve();
        builder.getBuilder().hearing(hearing).currentOwner(currentOwner).requestor(requestor)
            .attempts(attempts).startTime(startTime)
            .endTime(endTime).createdBy(createdBy).lastModifiedBy(lastModifiedBy).createdAt(createdAt)
            .lastModifiedAt(lastModifiedAt)
            .status(MediaRequestStatus.PROCESSING)
            .requestType(AudioRequestType.PLAYBACK);
        return builder;
    }

    public CustomMediaRequestEntity.CustomMediaBuilderRetrieve someMaximal() {
        return someMinimalRequestData();
    }

    /**
     * @deprecated do not use. Instead, use someMinimal().
     */
    @Deprecated
    public CustomMediaRequestEntity.CustomMediaBuilderRetrieve someMinimalRequestData() {
        return someMinimal();
    }

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status) {
        CustomMediaRequestEntity.CustomMediaBuilderRetrieve builder = someMinimalRequestData();

        builder.getBuilder()
                   .hearing(hearingEntity)
                   .requestor(requestor)
                   .currentOwner(requestor)
                   .startTime(startTime)
                   .endTime(endTime)
                   .requestType(audioRequestType)
                   .status(status)
                   .build();

        return builder.build();
}

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status, OffsetDateTime requestedDate) {
        CustomMediaRequestEntity.CustomMediaBuilderRetrieve builder = new CustomMediaRequestEntity.CustomMediaBuilderRetrieve();

         builder.getBuilder()
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

         return builder.build();
    }
}
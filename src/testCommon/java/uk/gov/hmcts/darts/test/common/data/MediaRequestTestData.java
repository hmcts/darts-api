package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestMediaRequestEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

@SuppressWarnings("SummaryJavadoc")
public class MediaRequestTestData implements Persistable<TestMediaRequestEntity.TestMediaBuilderRetrieve, MediaRequestEntity,
    TestMediaRequestEntity.TestMediaRequestEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime YESTERDAY = NOW.minusDays(1);

    private Integer id;

    private HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();

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

    public MediaRequestEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    /**
     * @deprecated do not use. Instead, use someMinimal().
     */
    @Deprecated
    public TestMediaRequestEntity.TestMediaBuilderRetrieve someMinimalRequestData() {
        return someMinimalBuilderHolder();
    }

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status) {
        TestMediaRequestEntity.TestMediaBuilderRetrieve builder = someMinimalRequestData();

        builder.getBuilder()
                   .hearing(hearingEntity)
                   .requestor(requestor)
                   .currentOwner(requestor)
                   .startTime(startTime)
                   .endTime(endTime)
                   .requestType(audioRequestType)
                   .status(status)
                   .build();

        return builder.build().getEntity();
}

    /**
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status, OffsetDateTime requestedDate) {
        TestMediaRequestEntity.TestMediaBuilderRetrieve builder = new TestMediaRequestEntity.TestMediaBuilderRetrieve();

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

         return builder.build().getEntity();
    }

    @Override
    public TestMediaRequestEntity.TestMediaBuilderRetrieve someMinimalBuilderHolder() {
        TestMediaRequestEntity.TestMediaBuilderRetrieve builder = new TestMediaRequestEntity.TestMediaBuilderRetrieve();
        builder.getBuilder().hearing(hearing).currentOwner(currentOwner).requestor(requestor)
            .attempts(attempts).startTime(startTime)
            .endTime(endTime).createdBy(createdBy).lastModifiedBy(lastModifiedBy).createdAt(createdAt)
            .lastModifiedAt(lastModifiedAt)
            .status(MediaRequestStatus.PROCESSING)
            .requestType(AudioRequestType.PLAYBACK);
        return builder;
    }

    @Override
    public TestMediaRequestEntity.TestMediaRequestEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}
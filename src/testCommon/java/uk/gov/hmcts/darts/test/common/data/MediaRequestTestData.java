package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestMediaRequestEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class MediaRequestTestData implements Persistable<TestMediaRequestEntity.TestMediaBuilderRetrieve, MediaRequestEntity,
    TestMediaRequestEntity.TestMediaRequestEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime YESTERDAY = NOW.minusDays(1);

    private final HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();

    private final UserAccountEntity currentOwner = UserAccountTestData.minimalUserAccount();

    private final UserAccountEntity requestor = UserAccountTestData.minimalUserAccount();

    private final Integer attempts = 0;

    private final OffsetDateTime startTime = YESTERDAY;

    private final OffsetDateTime endTime = YESTERDAY.plusHours(1);

    private final UserAccountEntity createdBy = UserAccountTestData.minimalUserAccount();

    private final UserAccountEntity lastModifiedBy = UserAccountTestData.minimalUserAccount();

    private final OffsetDateTime createdAt = NOW;

    private final OffsetDateTime lastModifiedAt = NOW;

    MediaRequestTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public MediaRequestEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    /**
     * gets a minimal set of data.
     *
     * @deprecated do not use. Instead, use someMinimal().
     */
    @Deprecated
    public static MediaRequestEntity someMinimalRequestData() {
        var mediaRequest = new MediaRequestEntity();
        mediaRequest.setHearing(PersistableFactory.getHearingTestData().someMinimalHearing());
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

    /**
     * creates a current media request.
     *
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public static MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status) {
        return createCurrentMediaRequest(hearingEntity, requestor, requestor, startTime, endTime, audioRequestType, status, OffsetDateTime.now());
    }

    /**
     * Create the media request.
     *
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
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

    private static OffsetDateTime middayToday() {
        return OffsetDateTime.of(LocalDate.now(), LocalTime.of(12, 0), UTC);
    }
}
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
     * gets a minimal set of data.
     * @deprecated do not use. Instead, use someMinimal().
     */
    @Deprecated
    public MediaRequestEntity someMinimalRequestData() {
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
     * creates a current media reqyest.
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               AudioRequestType audioRequestType, MediaRequestStatus status) {
        return createCurrentMediaRequest(hearingEntity, requestor, requestor, startTime, endTime, audioRequestType, status, OffsetDateTime.now());
    }

    /**
     * Create the media request.
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public MediaRequestEntity createCurrentMediaRequest(HearingEntity hearingEntity, UserAccountEntity owner, UserAccountEntity requestor,
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
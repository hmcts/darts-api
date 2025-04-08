package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Deprecated
public class MediaRequestStub {

    private final HearingStub hearingStub;
    private final MediaRequestRepository mediaRequestRepository;
    private final UserAccountStub userAccountStub;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    @Transactional
    public MediaRequestEntity createAndLoadMediaRequestEntity(UserAccountEntity requestor,
                                                              AudioRequestType audioRequestType,
                                                              MediaRequestStatus status) {
        return createAndLoadMediaRequestEntity(requestor, requestor, audioRequestType, status, "NEWCASTLE", "2",
                                               LocalDateTime.of(2023, 6, 10, 10, 0, 0),
                                               OffsetDateTime.parse("2023-06-26T13:00:00Z"), OffsetDateTime.parse("2023-06-26T13:45:00Z"),
                                               OffsetDateTime.now());
    }

    @Transactional
    public MediaRequestEntity createAndLoadMediaRequestEntity(UserAccountEntity requestor,
                                                              HearingEntity hearing,
                                                              AudioRequestType audioRequestType,
                                                              MediaRequestStatus status) {
        return createAndLoadMediaRequestEntity(requestor, requestor, hearing, audioRequestType, status,
                                               OffsetDateTime.parse("2023-06-26T13:00:00Z"), OffsetDateTime.parse("2023-06-26T13:45:00Z"),
                                               OffsetDateTime.now());
    }

    @Transactional
    public MediaRequestEntity createAndLoadMediaRequestEntity(UserAccountEntity owner, UserAccountEntity requestor,
                                                              AudioRequestType audioRequestType,
                                                              MediaRequestStatus status, String courtName,
                                                              String caseNumber, LocalDateTime hearingDate,
                                                              OffsetDateTime startTime, OffsetDateTime endTime, OffsetDateTime requestedDate) {
        HearingEntity hearing = hearingStub.createHearing(courtName, "Int Test Courtroom 2",
                                                          caseNumber, hearingDate);

        return createAndLoadMediaRequestEntity(owner, requestor, hearing, audioRequestType, status, startTime, endTime, requestedDate);
    }

    @Transactional
    public MediaRequestEntity createAndLoadMediaRequestEntity(UserAccountEntity owner, UserAccountEntity requestor,
                                                              HearingEntity hearing,
                                                              AudioRequestType audioRequestType,
                                                              MediaRequestStatus status,
                                                              OffsetDateTime startTime, OffsetDateTime endTime, OffsetDateTime requestedDate) {
        var currentMediaRequest = PersistableFactory.getMediaRequestTestData().createCurrentMediaRequest(
            hearing,
            owner,
            requestor,
            startTime,
            endTime,
            audioRequestType, status, requestedDate
        );
        currentMediaRequest.setCreatedBy(requestor);
        currentMediaRequest.setCreatedDateTime(requestedDate);
        return dartsDatabaseSaveStub.save(currentMediaRequest);
    }

    @Transactional
    public MediaRequestEntity createAndSaveMediaRequestEntity(UserAccountEntity requestor) {
        return createAndLoadMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD, MediaRequestStatus.COMPLETED);
    }

    @Transactional
    public MediaRequestEntity createAndSaveMediaRequestEntity(MediaRequestStatus status) {
        return createAndLoadMediaRequestEntity(userAccountStub.getIntegrationTestUserAccountEntity(), AudioRequestType.DOWNLOAD, status);
    }

    @Transactional
    public MediaRequestEntity createAndSaveMediaRequestEntity(UserAccountEntity createdBy, HearingEntity hearingEntity) {
        return createAndLoadMediaRequestEntity(createdBy, hearingEntity, AudioRequestType.DOWNLOAD, MediaRequestStatus.COMPLETED);
    }

    @Transactional
    public Optional<MediaRequestEntity> getFindId(MediaRequestEntity mediaRequestEntity) {
        return mediaRequestRepository.findById(mediaRequestEntity.getId());
    }

}
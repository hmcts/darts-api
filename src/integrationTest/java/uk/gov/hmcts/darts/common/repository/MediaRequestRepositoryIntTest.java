package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;

class MediaRequestRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    MediaRequestStub mediaRequestStub;

    @Autowired
    MediaRequestRepository mediaRequestRepository;
    
    @Autowired
    UserAccountStub userAccountStub;
    
    @Autowired
    TransformedMediaStub transformedMediaStub;


    @Test
    void cleanupStuckRequests_shouldOnlyUpdateProcessingStatusThatWereLastModifiedAfterTheDateProvided() {

        MediaRequestEntity mediaRequest1ShouldUpdateMoreThan24HOld = PersistableFactory.getMediaRequestTestData().someMinimal();
        MediaRequestEntity mediaRequest2ShouldUpdateMoreThen24HOld = PersistableFactory.getMediaRequestTestData().someMinimal();
        MediaRequestEntity mediaRequest3ShouldNotUpdateLessThen24HOld = PersistableFactory.getMediaRequestTestData().someMinimal();
        MediaRequestEntity mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus = PersistableFactory.getMediaRequestTestData().someMinimal();

        mediaRequest1ShouldUpdateMoreThan24HOld.setStatus(PROCESSING);
        mediaRequest2ShouldUpdateMoreThen24HOld.setStatus(PROCESSING);
        mediaRequest3ShouldNotUpdateLessThen24HOld.setStatus(PROCESSING);
        mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus.setStatus(COMPLETED);

        dartsPersistence.save(mediaRequest1ShouldUpdateMoreThan24HOld);
        dartsPersistence.save(mediaRequest2ShouldUpdateMoreThen24HOld);
        dartsPersistence.save(mediaRequest3ShouldNotUpdateLessThen24HOld);
        dartsPersistence.save(mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus);

        dartsPersistence.overrideLastModifiedBy(mediaRequest1ShouldUpdateMoreThan24HOld, OffsetDateTime.now().minusHours(25));
        dartsPersistence.overrideLastModifiedBy(mediaRequest2ShouldUpdateMoreThen24HOld, OffsetDateTime.now().minusHours(25));
        dartsPersistence.overrideLastModifiedBy(mediaRequest3ShouldNotUpdateLessThen24HOld, OffsetDateTime.now().minusHours(23));
        dartsPersistence.overrideLastModifiedBy(mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus, OffsetDateTime.now().minusHours(25));

        mediaRequestRepository.cleanupStuckRequests(OffsetDateTime.now().minusDays(1));

        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest1ShouldUpdateMoreThan24HOld.getId()).orElseThrow()
                       .getStatus()).isEqualTo(FAILED);
        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest2ShouldUpdateMoreThen24HOld.getId()).orElseThrow()
                       .getStatus()).isEqualTo(FAILED);
        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest3ShouldNotUpdateLessThen24HOld.getId()).orElseThrow()
                       .getStatus()).isEqualTo(PROCESSING);
        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus.getId()).orElseThrow()
                       .getStatus()).isEqualTo(COMPLETED);
    }


    @Test
    void testUpdateAndRetrieveMediaRequestToProcessingIgnoresCompletedMediaRequest() {
        MediaRequestEntity mediaRequestEntity = mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        var mediaRequest1 = mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);
        mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);
        mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);

        Integer updatedMediaRequestId = mediaRequestRepository.updateAndRetrieveMediaRequestToProcessing(mediaRequestEntity.getLastModifiedById(),
                                                                                                         List.of(0));

        MediaRequestEntity updatedMediaRequest = mediaRequestRepository.findById(updatedMediaRequestId).orElseThrow();
        assertThat(updatedMediaRequest.getId()).isEqualTo(mediaRequest1.getId());
        assertThat(updatedMediaRequest.getStatus()).isEqualTo(PROCESSING);
    }

    @Test
    void testUpdateAndRetrieveMediaRequestToProcessingReturnsNullIfNoOpenMediaRequests() {
        MediaRequestEntity mediaRequestEntity = mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);

        var updatedMediaRequest = mediaRequestRepository.updateAndRetrieveMediaRequestToProcessing(mediaRequestEntity.getLastModifiedById(),
                                                                                                   List.of(0));

        assertThat(updatedMediaRequest).isNull();
    }
    
    @Test
    void countTransformedEntitiesByCurrentOwnerIdAndStatusNotAccessed_ReturnsNotAccessedCount_WhenUserOwnsTransformedMedia() {
        UserAccountEntity owner = userAccountStub.createIntegrationUser(null,"Test Owner",  "testowner@gmail.com", true);
        UserAccountEntity requestor = userAccountStub.createIntegrationUser(null, "Test Requestor",  "testrequestor@gmail.com", true);

        MediaRequestEntity mediaRequestEntity1 = mediaRequestStub.createAndLoadMediaRequestEntity(requestor, requestor, AudioRequestType.DOWNLOAD, COMPLETED);
        MediaRequestEntity mediaRequestEntity2 = mediaRequestStub.createAndLoadMediaRequestEntity(owner, requestor, AudioRequestType.DOWNLOAD, COMPLETED);
        MediaRequestEntity mediaRequestEntity3 = mediaRequestStub.createAndLoadMediaRequestEntity(owner, requestor, AudioRequestType.DOWNLOAD, COMPLETED);

        transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity1, "test file 1", OffsetDateTime.now().plusDays(2), null);
        transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity2, "test file 2", OffsetDateTime.now().plusDays(2), null);
        transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity3, "test file 3", OffsetDateTime.now().plusDays(2), null);
        
        long notAccessedCount = mediaRequestRepository.countTransformedEntitiesByCurrentOwnerIdAndStatusNotAccessed(
            owner.getId(),
            COMPLETED
        );
        
        assertThat(notAccessedCount).isEqualTo(2);
    }

}
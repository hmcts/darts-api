package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;

class MediaRequestRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    MediaRequestStub mediaRequestStub;

    @Autowired
    MediaRequestRepository mediaRequestRepository;

    @Test
    void testUpdateAndRetrieveMediaRequestToProcessingIgnoresCompletedMediaRequest() {
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        var mediaRequest1 = mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);
        mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);
        mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);

        var updatedMediaRequest = mediaRequestRepository.updateAndRetrieveMediaRequestToProcessing();

        assertThat(updatedMediaRequest.getId()).isEqualTo(mediaRequest1.getId());
        assertThat(updatedMediaRequest.getStatus()).isEqualTo(PROCESSING);
    }

    @Test
    void testUpdateAndRetrieveMediaRequestToProcessingReturnsNullIfNoOpenMediaRequests() {
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);

        var updatedMediaRequest = mediaRequestRepository.updateAndRetrieveMediaRequestToProcessing();

        assertThat(updatedMediaRequest).isNull();
    }

}

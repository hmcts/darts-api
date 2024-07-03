package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.testutils.RepositoryBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;

class MediaRequestRepositoryIntTest extends RepositoryBase {

    @Autowired
    MediaRequestStub mediaRequestStub;

    @Autowired
    MediaRequestRepository mediaRequestRepository;

    @Test
    void testCompletedMediaRequestsAreIgnoredForProcessing() {
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        var mediaRequest1 = mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);
        mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);
        mediaRequestStub.createAndSaveMediaRequestEntity(OPEN);

        var mediaRequestToProcess = mediaRequestRepository.updateAndRetrieveOldestOpenMediaRequestToProcessing();

        assertThat(mediaRequestToProcess.getId()).isEqualTo(mediaRequest1.getId());
    }

    @Test
    void testNoOpenMediaRequestsReturnsNoResultForProcessing() {
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);
        mediaRequestStub.createAndSaveMediaRequestEntity(COMPLETED);

        var mediaRequestToProcess = mediaRequestRepository.updateAndRetrieveOldestOpenMediaRequestToProcessing();

        assertThat(mediaRequestToProcess).isNull();
    }

}

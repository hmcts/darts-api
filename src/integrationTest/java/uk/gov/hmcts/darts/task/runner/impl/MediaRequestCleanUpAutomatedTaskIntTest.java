package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MediaRequestCleanUpAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MediaRequestCleanUpAutomatedTaskIntTest extends PostgresIntegrationBase {

    private final MediaRequestCleanUpAutomatedTask mediaRequestCleanUpAutomatedTask;

    @Test
    void runTask_shouldUpdatePorcessingToBeFailedAfter24Hours() {
        MediaRequestEntity mediaRequest1ShouldUpdateMoreThen24HOld = PersistableFactory.getMediaRequestTestData().someMinimal();
        MediaRequestEntity mediaRequest2ShouldUpdateMoreThen24HOld = PersistableFactory.getMediaRequestTestData().someMinimal();
        MediaRequestEntity mediaRequest3ShouldNotUpdateLessThen24HOld = PersistableFactory.getMediaRequestTestData().someMinimal();
        MediaRequestEntity mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus = PersistableFactory.getMediaRequestTestData().someMinimal();

        mediaRequest1ShouldUpdateMoreThen24HOld.setStatus(MediaRequestStatus.PROCESSING);
        mediaRequest2ShouldUpdateMoreThen24HOld.setStatus(MediaRequestStatus.PROCESSING);
        mediaRequest3ShouldNotUpdateLessThen24HOld.setStatus(MediaRequestStatus.PROCESSING);
        mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus.setStatus(MediaRequestStatus.COMPLETED);

        dartsPersistence.save(mediaRequest1ShouldUpdateMoreThen24HOld);
        dartsPersistence.save(mediaRequest2ShouldUpdateMoreThen24HOld);
        dartsPersistence.save(mediaRequest3ShouldNotUpdateLessThen24HOld);
        dartsPersistence.save(mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus);

        dartsPersistence.overrideLastModifiedBy(mediaRequest1ShouldUpdateMoreThen24HOld, OffsetDateTime.now().minusHours(25));
        dartsPersistence.overrideLastModifiedBy(mediaRequest2ShouldUpdateMoreThen24HOld, OffsetDateTime.now().minusHours(25));
        dartsPersistence.overrideLastModifiedBy(mediaRequest3ShouldNotUpdateLessThen24HOld, OffsetDateTime.now().minusHours(23));
        dartsPersistence.overrideLastModifiedBy(mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus, OffsetDateTime.now().minusHours(25));


        mediaRequestCleanUpAutomatedTask.runTask();

        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest1ShouldUpdateMoreThen24HOld.getId()).orElseThrow()
                       .getStatus()).isEqualTo(MediaRequestStatus.FAILED);
        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest2ShouldUpdateMoreThen24HOld.getId()).orElseThrow()
                       .getStatus()).isEqualTo(MediaRequestStatus.FAILED);
        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest3ShouldNotUpdateLessThen24HOld.getId()).orElseThrow()
                       .getStatus()).isEqualTo(MediaRequestStatus.PROCESSING);
        assertThat(dartsDatabase.getMediaRequestRepository().findById(mediaRequest4ShouldNotUpdateMoreThen24HOldButNotFailedStatus.getId()).orElseThrow()
                       .getStatus()).isEqualTo(MediaRequestStatus.COMPLETED);

    }
}

package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.TranscriptionHubRequestEntity;

public interface TranscriptionHubRequestRepository extends JpaRepository<TranscriptionHubRequestEntity, Long> {
}

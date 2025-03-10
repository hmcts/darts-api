package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.TranscriptionLinkedCaseEntity;

public interface TranscriptionLinkedCaseRepository extends JpaRepository<TranscriptionLinkedCaseEntity, Integer> {
}

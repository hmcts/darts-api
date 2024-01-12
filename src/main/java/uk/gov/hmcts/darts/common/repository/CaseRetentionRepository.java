package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRetentionRepository  extends JpaRepository<CaseRetentionEntity, Integer> {
    List<CaseRetentionEntity> findAllByCourtCase(CourtCaseEntity courtCase);

    Optional<CaseRetentionEntity> findTopByCourtCaseAndCurrentStateOrderByCreatedDateTimeDesc(CourtCaseEntity courtCase, String currentState);
}

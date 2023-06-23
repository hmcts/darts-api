package uk.gov.hmcts.darts.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Case;
import uk.gov.hmcts.darts.common.entity.EventType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {

    Optional<Case> findByCaseId(String caseNumber); // Should be caseNumber
}

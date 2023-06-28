package uk.gov.hmcts.darts.cases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseEntity;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, Integer> {

    CaseEntity findByCaseNumber(String caseNumber);
}

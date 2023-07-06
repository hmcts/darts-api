package uk.gov.hmcts.darts.cases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseEntity;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, Integer> {

    List<CaseEntity> findByCaseNumber(String caseNumber);

    Optional<CaseEntity> findByCaseNumberAndCourthouse_CourthouseName(String caseNumber, String courthouseName);
}

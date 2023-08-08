package uk.gov.hmcts.darts.cases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.Optional;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface CaseRepository extends JpaRepository<CourtCaseEntity, Integer> {

    Optional<CourtCaseEntity> findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(String caseNumber, String courthouseName);

    @Override
    Optional<CourtCaseEntity> findById(Integer id);


}

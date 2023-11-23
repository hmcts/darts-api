package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface CaseRepository extends JpaRepository<CourtCaseEntity, Integer> {

    Optional<CourtCaseEntity> findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(String caseNumber,
                                                                                               String courthouseName);

    @Query("""
        SELECT case.caseNumber
        FROM CourtCaseEntity case
        WHERE case.closed = false
        and case.caseNumber in :caseNumbers
        and upper(case.courthouse.courthouseName) = upper(:courthouseName)
        """)
    List<String> findOpenCaseNumbers(String courthouseName, List<String> caseNumbers);

}

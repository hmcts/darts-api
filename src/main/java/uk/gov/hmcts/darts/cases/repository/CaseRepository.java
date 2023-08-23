package uk.gov.hmcts.darts.cases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface CaseRepository extends JpaRepository<CourtCaseEntity, Integer> {

    Optional<CourtCaseEntity> findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(String caseNumber, String courthouseName);

    @Override
    Optional<CourtCaseEntity> findById(Integer id);

    @Query("""
        SELECT courthouse FROM CourtCaseEntity courtCase
            JOIN courtCase.courthouse courthouse
    """)
    List<CourthouseEntity> getAssociatedCourthouses(Integer caseId);

}

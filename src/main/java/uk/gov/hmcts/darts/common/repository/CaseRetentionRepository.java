package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRetentionRepository extends JpaRepository<CaseRetentionEntity, Integer> {
    List<CaseRetentionEntity> findAllByCourtCase(CourtCaseEntity courtCase);

    List<CaseRetentionEntity> findByCourtCase_Id(Integer courtCaseId);

    Optional<CaseRetentionEntity> findTopByCourtCaseAndCurrentStateOrderByCreatedDateTimeDesc(CourtCaseEntity courtCase, String currentState);

    @Query("""
        SELECT c
        FROM CaseRetentionEntity c
        WHERE courtCase = :courtCase
        AND manualOverride=false
        AND currentState='COMPLETE'
        ORDER BY c.createdDateTime desc
        limit 1
        """
    )
    Optional<CaseRetentionEntity> findLatestCompletedAutomatedRetention(CourtCaseEntity courtCase);

    @Query("""
        SELECT c
        FROM CaseRetentionEntity c
        WHERE courtCase = :courtCase
        AND currentState='COMPLETE'
        ORDER BY c.createdDateTime desc
        limit 1
        """
    )
    Optional<CaseRetentionEntity> findLatestCompletedRetention(CourtCaseEntity courtCase);

    @Query("""
        SELECT c
        FROM CaseRetentionEntity c, CourtCaseEntity case
        WHERE case.id = :caseId
        AND c.courtCase = case
        ORDER BY c.createdDateTime
        """
    )
    List<CaseRetentionEntity> findByCaseId(Integer caseId);
}

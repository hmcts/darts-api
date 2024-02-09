package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.event.model.stopandclosehandler.PendingRetention;

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
            WHERE c.courtCase = :courtCase
            and c.retentionPolicyType.fixedPolicyKey not in ('PERM', 'MANUAL')
            AND c.currentState='COMPLETE'
            ORDER BY c.createdDateTime desc
            limit 1
            """
    )
    Optional<CaseRetentionEntity> findLatestCompletedAutomatedRetention(CourtCaseEntity courtCase);

    @Query("""
            SELECT c
            FROM CaseRetentionEntity c
            WHERE c.courtCase = :courtCase
            and c.retentionPolicyType.fixedPolicyKey in ('PERM', 'MANUAL')
            AND c.currentState='COMPLETE'
            ORDER BY c.createdDateTime desc
            limit 1
            """
    )
    Optional<CaseRetentionEntity> findLatestCompletedManualRetention(CourtCaseEntity courtCase);

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


    @Query("""
            SELECT new uk.gov.hmcts.darts.event.model.stopandclosehandler.PendingRetention (
            cr,
            e.timestamp)

            FROM CaseRetentionEntity cr, CaseManagementRetentionEntity cmr, EventEntity e
            WHERE cr.caseManagementRetention = cmr
            and cmr.eventEntity = e
            and cr.currentState = 'PENDING'
            and cr.courtCase = :courtCase
            order by e.timestamp desc
            LIMIT 1
            """)
    Optional<PendingRetention> findLatestPendingRetention(CourtCaseEntity courtCase);

}

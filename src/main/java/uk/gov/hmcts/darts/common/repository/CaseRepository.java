package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface CaseRepository extends JpaRepository<CourtCaseEntity, Integer> {

    Optional<CourtCaseEntity> findByCaseNumberAndCourthouse_CourthouseName(String caseNumber,
                                                                           String courthouseName);

    Optional<CourtCaseEntity> findByCaseNumberAndCourthouse(String caseNumber,
                                                            CourthouseEntity courthouse);

    @Query("""
        SELECT case.caseNumber
        FROM CourtCaseEntity case
        WHERE case.closed = false
        and case.caseNumber in :caseNumbers
        and case.courthouse.courthouseName = upper(:courthouseName)
        """)
    List<String> findOpenCaseNumbers(String courthouseName, List<String> caseNumbers);

    @Query("""
        select exists(
        SELECT cc.id FROM CourtCaseEntity cc
        WHERE cc.courthouse.id = :courthouseId)
        """)
    boolean caseExistsForCourthouse(Integer courthouseId);

    boolean existsByCourthouse(CourthouseEntity courthouse);

    @Query("""
        SELECT ce.id FROM CourtCaseEntity ce
        WHERE ce.createdDateTime < :cutoffDate
        AND ce.closed = false
        AND NOT EXISTS (select 1 from CaseRetentionEntity cre
            where cre.courtCase.id = ce.id)
        ORDER BY ce.createdDateTime ASC
        """)
    List<CourtCaseEntity> findOpenCasesToClose(OffsetDateTime cutoffDate, Limit limit);

    List<CourtCaseEntity> findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(int maxRetentionRetries, Limit limit);

    @Query("""
        SELECT c FROM CourtCaseEntity c
        WHERE c.closed = true
        AND c.caseClosedTimestamp <= :caseClosedBeforeTimestamp
        AND NOT EXISTS (select cde from CaseDocumentEntity cde
            where (cde.courtCase.id = c.id))
        """)
    List<CourtCaseEntity> findCasesNeedingCaseDocumentGenerated(OffsetDateTime caseClosedBeforeTimestamp, Pageable pageable);

    @Query("""
            SELECT cc.id
            FROM CourtCaseEntity cc,
            CaseRetentionEntity cr,
            (select cr2.courtCase.id as caseId, max(cr2.createdDateTime) latest_ts
                FROM CaseRetentionEntity cr2
                WHERE cr2.currentState = 'COMPLETE'
                GROUP by cr2.courtCase.id) latest_case_retention
            WHERE cr.courtCase.id = cc.id
            AND latest_case_retention.latest_ts = cr.createdDateTime
            AND latest_case_retention.caseId = cr.courtCase.id
            AND cr.retainUntil between CURRENT_TIMESTAMP and :retainUntilTimestamp
            AND not exists (select 1 from CaseDocumentEntity cd
                WHERE cd.courtCase.id = cc.id
                AND cd.createdDateTime >= :caseDocumentCreatedAfterTimestamp
            )
            ORDER BY cc.isRetentionUpdated ASC, cc.id ASC
        """)
    List<Integer> findCasesNeedingCaseDocumentForRetentionDateGeneration(OffsetDateTime retainUntilTimestamp,
                                                                         OffsetDateTime caseDocumentCreatedAfterTimestamp,
                                                                         Pageable pageable);

    @Query(value = """
        select cc.id from CourtCaseEntity cc
        join CaseRetentionEntity cr
        on cr.courtCase.id = cc.id and cr.currentState = 'COMPLETE'
        where cc.isDataAnonymised = false
        and cr.retainUntil < :maxRetentionDate
        """)
    List<Integer> findCaseIdsToBeAnonymised(OffsetDateTime maxRetentionDate, Limit limit);
}

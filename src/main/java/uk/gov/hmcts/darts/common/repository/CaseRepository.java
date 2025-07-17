package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({
    "PMD.MethodNamingConventions",
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
@Repository
public interface CaseRepository
    extends JpaRepository<CourtCaseEntity, Integer>, JpaSpecificationExecutor<CourtCaseEntity> {

    Optional<CourtCaseEntity> findByCaseNumberAndCourthouse_CourthouseName(String caseNumber,
                                                                           String courthouseName);

    Optional<CourtCaseEntity> findByCaseNumberAndCourthouse(String caseNumber,
                                                            CourthouseEntity courthouse);

    @Query("""
        SELECT c.caseNumber
        FROM CourtCaseEntity c
        WHERE c.closed = false
        and c.caseNumber in :caseNumbers
        and c.courthouse.courthouseName = upper(trim(:courthouseName))
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
    List<Integer> findOpenCasesToClose(OffsetDateTime cutoffDate, Limit limit);

    @Query("""
        SELECT distinct cc.id
        FROM CourtCaseEntity cc
        JOIN cc.caseRetentionEntities cr                
        WHERE cr is not null
        AND cc.isRetentionUpdated = true
        AND cc.retentionRetries < :maxRetentionRetries
        ORDER BY cc.id ASC
        """)
    List<Integer> findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(int maxRetentionRetries, Limit limit);

    @Query("""
        SELECT cc.id 
        FROM CourtCaseEntity cc,
        CaseRetentionEntity cr,
        (select cr2.courtCase.id as caseId, max(cr2.createdDateTime) latest_ts
                FROM CaseRetentionEntity cr2
                WHERE cr2.currentState = 'COMPLETE'
                GROUP by cr2.courtCase.id) latest_case_retention
        WHERE cr.courtCase.id = cc.id
        AND cc.closed = true
        AND cc.caseClosedTimestamp <= :caseClosedBeforeTimestamp
        AND latest_case_retention.latest_ts = cr.createdDateTime
        AND latest_case_retention.caseId = cr.courtCase.id
        AND cr.retainUntil is not null
        AND NOT EXISTS 
            (select cde FROM CaseDocumentEntity cde WHERE (cde.courtCase.id = cc.id))
        """)
    List<Integer> findCasesIdsNeedingCaseDocumentGenerated(OffsetDateTime caseClosedBeforeTimestamp, Limit limit);

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
                                                                         Limit limit);

    @Query("""
        select cc.id from CourtCaseEntity cc
        join CaseRetentionEntity cr
        on cr.courtCase.id = cc.id
        where cr.currentState = 'COMPLETE'
        and cr.createdDateTime = (
            select MAX(sub_cr.createdDateTime) from CaseRetentionEntity sub_cr
            where sub_cr.courtCase.id = cc.id
            and sub_cr.currentState = 'COMPLETE'
        )
        and cc.isDataAnonymised = false
        and cr.retainUntil < :maxRetentionDate
        """)
    List<Integer> findCaseIdsToBeAnonymised(OffsetDateTime maxRetentionDate, Limit limit);


    @Query("""
        SELECT cc
        FROM CourtCaseEntity cc
        WHERE cc.id IN :ids
        ORDER BY cc.courthouse.courthouseName ASC,
            cc.caseNumber ASC
        """)
    List<CourtCaseEntity> findAllWithIdMatchingOneOf(List<Integer> ids);
}

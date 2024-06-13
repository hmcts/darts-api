package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
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

    @Query("""
        select exists(
        SELECT cc.id FROM CourtCaseEntity cc
        WHERE cc.courthouse.id = :courthouseId)
        """)
    boolean caseExistsForCourthouse(Integer courthouseId);

    boolean existsByCourthouse(CourthouseEntity courthouse);

    @Query("""
        SELECT case FROM CourtCaseEntity case
        WHERE case.createdDateTime < :cutoffDate
        AND case.closed = false
        AND NOT EXISTS (select 1 from CaseRetentionEntity cre
            where cre.courtCase.id = case.id)
        """)
    List<CourtCaseEntity> findOpenCasesToClose(OffsetDateTime cutoffDate);

    List<CourtCaseEntity> findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(int maxRetentionRetries);

    @Query("""
        SELECT case FROM CourtCaseEntity case
        WHERE case.closed = true
        AND case.caseClosedTimestamp <= :caseClosedBeforeTimestamp
        AND NOT EXISTS (select cde from CaseDocumentEntity cde
            where (cde.courtCase.id = case.id))
        """)
    List<CourtCaseEntity> findCasesNeedingCaseDocumentGenerated(OffsetDateTime caseClosedBeforeTimestamp, Pageable pageable);

    @EntityGraph(value = "CourtCase.caseDocument", type = EntityGraphType.LOAD)
//    @Query("""
//        SELECT case FROM CourtCaseEntity case
//        WHERE case.id = :courtCaseId
//        """)
    Optional<CourtCaseEntity> findById(Integer courtCaseId);

    @EntityGraph(value = "CourtCase.caseDocument", type = EntityGraphType.LOAD)
    @Query("""
        SELECT case FROM CourtCaseEntity case
        WHERE case.id = :courtCaseId
        """)
    Optional<CourtCaseEntity> loadCaseForCaseDocument(Integer courtCaseId);
}

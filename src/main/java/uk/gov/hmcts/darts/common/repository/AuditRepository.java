package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AuditEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditEntity, Integer>, JpaSpecificationExecutor<AuditEntity> {
    @Query("""
        SELECT audit FROM AuditEntity audit
        Where audit.courtCase.id = :caseId
        AND audit.auditActivity.id = :activityId
        AND audit.createdDateTime between :fromDate AND :toDate
        """)
    List<AuditEntity> getAuditEntitiesByCaseAndActivityForDateRange(Integer caseId,
                                                                    Integer activityId,
                                                                    OffsetDateTime fromDate,
                                                                    OffsetDateTime toDate);
}
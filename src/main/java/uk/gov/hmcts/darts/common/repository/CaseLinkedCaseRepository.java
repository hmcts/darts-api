package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;

@Repository
public interface CaseLinkedCaseRepository extends JpaRepository<CaseLinkedCaseEntity, Integer>,
    JpaSpecificationExecutor<CaseLinkedCaseEntity> {

    @Query("""
        SELECT clc
        FROM CaseLinkedCaseEntity clc
        WHERE clc.courtCase1 = :courtCase
        OR clc.courtCase2 = :courtCase
        """)
    List<CaseLinkedCaseEntity> findByCourtCase(@Param("courtCase") CourtCaseEntity courtCase);

}

package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;

import java.util.List;

@Repository
public interface EventLinkedCaseRepository extends JpaRepository<EventLinkedCaseEntity, Integer> {

    List<EventLinkedCaseEntity> findAllByCourtCase(CourtCaseEntity courtCase);

    List<EventLinkedCaseEntity> findAllByCaseNumberAndCourthouseName(String caseNumber, String courthouseName);

    @Query("""
        SELECT COUNT(DISTINCT cc) = (COUNT(cc.isDataAnonymised) filter (where cc.isDataAnonymised = true))
                    FROM EventLinkedCaseEntity elc 
                    LEFT JOIN CourtCaseEntity cc ON elc.courtCase = cc 
                    WHERE elc.event = :eventEntity
                    group by elc.event
        """
    )
    boolean areAllAssociatedCasesAnonymised(EventEntity eventEntity);

    @Transactional
    @Modifying
    @Query("DELETE FROM EventLinkedCaseEntity elc WHERE elc.event.id IN :eventsIds")
    void deleteAllByEventIn(List<Integer> eventsIds);
}
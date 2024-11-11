package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;

import java.util.List;

@Repository
public interface EventLinkedCaseRepository extends JpaRepository<EventLinkedCaseEntity, Integer> {

    List<EventLinkedCaseEntity> findAllByCourtCase(CourtCaseEntity courtCase);

    List<EventLinkedCaseEntity> findAllByCaseNumberAndCourthouseName(String caseNumber, String courthouseName);
}
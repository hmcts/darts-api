package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;

public interface CaseDocumentRepository extends JpaRepository<CaseDocumentEntity, Integer> {

    List<CaseDocumentEntity> findByCourtCase(CourtCaseEntity courtCase);
}

package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;

public interface CaseDocumentRepository extends JpaRepository<CaseDocumentEntity, Integer> {
}

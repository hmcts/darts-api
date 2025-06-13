package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;

public interface CaseDocumentRepository extends JpaRepository<CaseDocumentEntity, Long>,
    SoftDeleteRepository<CaseDocumentEntity, Long> {
}

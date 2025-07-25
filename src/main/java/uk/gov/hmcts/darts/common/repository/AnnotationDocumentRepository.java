package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;

@Repository
public interface AnnotationDocumentRepository extends JpaRepository<AnnotationDocumentEntity, Long>,
    SoftDeleteRepository<AnnotationDocumentEntity, Long> {

}

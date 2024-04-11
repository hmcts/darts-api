package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;

import java.util.List;

@Repository
public interface AnnotationDocumentRepository extends JpaRepository<AnnotationDocumentEntity, Integer> {


    @Query("""
           SELECT annDoc
           FROM CourtCaseEntity ca
           JOIN ca.hearings he
           JOIN he.annotations ann
           JOIN ann.annotationDocuments annDoc
           WHERE ca.id = :caseId
        """)
    List<AnnotationDocumentEntity> findAllByCaseId(Integer caseId);

}

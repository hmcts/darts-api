package uk.gov.hmcts.darts.annotation.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.annotations.model.AnnotationDocument;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;

@UtilityClass
public class AnnotationDocumentMapper {

    public AnnotationDocument map(AnnotationDocumentEntity annotationDocumentEntity) {
        AnnotationDocument annotationDocument = new AnnotationDocument();
        annotationDocument.setAnnotationDocumentId(annotationDocumentEntity.getId());
        annotationDocument.setFileName(annotationDocumentEntity.getFileName());
        annotationDocument.setFileType(annotationDocumentEntity.getFileType());
        annotationDocument.setUploadedBy(annotationDocumentEntity.getUploadedBy().getUserFullName());
        annotationDocument.setUploadedTs(annotationDocumentEntity.getUploadedDateTime());

        return annotationDocument;
    }
}

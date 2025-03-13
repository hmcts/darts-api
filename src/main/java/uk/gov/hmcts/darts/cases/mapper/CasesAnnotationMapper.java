package uk.gov.hmcts.darts.cases.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.cases.model.AnnotationDocument;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

@Component
public class CasesAnnotationMapper {

    public Annotation map(HearingEntity hearingEntity, AnnotationEntity annotationEntity) {
        Annotation annotation = new Annotation();
        annotation.setAnnotationId(annotationEntity.getId());
        annotation.setHearingId(hearingEntity.getId());
        annotation.setHearingDate(hearingEntity.getHearingDate());
        annotation.setAnnotationTs(annotationEntity.getTimestamp());
        annotation.setAnnotationText(annotationEntity.getText());
        annotation.setAnnotationDocuments(
            annotationEntity.getAnnotationDocuments()
                .stream()
                .map(this::map)
                .toList());

        return annotation;
    }


    private AnnotationDocument map(AnnotationDocumentEntity annotationDocumentEntity) {
        AnnotationDocument annotationDocument = new AnnotationDocument();
        annotationDocument.setAnnotationDocumentId(annotationDocumentEntity.getId());
        annotationDocument.setFileName(annotationDocumentEntity.getFileName());
        annotationDocument.setFileType(annotationDocumentEntity.getFileType());
        annotationDocument.setUploadedBy(annotationDocumentEntity.getUploadedBy().getUserFullName());
        annotationDocument.setUploadedTs(annotationDocumentEntity.getUploadedDateTime());

        return annotationDocument;
    }
}

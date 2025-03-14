package uk.gov.hmcts.darts.hearings.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.Annotation;
import uk.gov.hmcts.darts.hearings.model.AnnotationDocument;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
public class GetAnnotationsResponseMapper {

    public List<Annotation> mapToAnnotations(List<AnnotationEntity> annotationEntities, Integer hearingId) {
        return emptyIfNull(annotationEntities)
            .stream()
            .map(annotationEntity -> mapToAnnotation(annotationEntity, hearingId))
            .toList();
    }

    private Annotation mapToAnnotation(AnnotationEntity annotationEntity, Integer hearingId) {
        Annotation annotation = new Annotation();
        annotation.setAnnotationId(annotationEntity.getId());
        annotation.setAnnotationTs(annotationEntity.getTimestamp());
        annotation.setAnnotationText(annotationEntity.getText());
        HearingEntity hearingEntity = findHearingInList(annotationEntity.getHearingList(), hearingId);
        annotation.setHearingId(hearingEntity.getId());
        annotation.setHearingDate(hearingEntity.getHearingDate());
        annotation.setAnnotationDocuments(mapToAnnotationDocuments(annotationEntity.getAnnotationDocuments()));
        return annotation;
    }

    private HearingEntity findHearingInList(List<HearingEntity> hearingEntities, Integer hearingId) {
        return hearingEntities
            .stream()
            .filter(hearing -> hearing.getId().equals(hearingId))
            .findAny()
            .orElse(new HearingEntity());
    }

    private List<AnnotationDocument> mapToAnnotationDocuments(List<AnnotationDocumentEntity> annotationDocumentEntities) {
        return emptyIfNull(annotationDocumentEntities)
            .stream()
            .map(GetAnnotationsResponseMapper::mapToAnnotationDocument)
            .collect(Collectors.toList());
    }

    private AnnotationDocument mapToAnnotationDocument(AnnotationDocumentEntity annotationDocumentEntity) {
        AnnotationDocument annotationDocument = new AnnotationDocument();
        annotationDocument.setAnnotationDocumentId(annotationDocumentEntity.getId());
        annotationDocument.setFileName(annotationDocumentEntity.getFileName());
        annotationDocument.setFileType(annotationDocumentEntity.getFileType());
        annotationDocument.setUploadedBy(annotationDocumentEntity.getUploadedBy().getUserFullName());
        annotationDocument.setUploadedTs(annotationDocumentEntity.getUploadedDateTime());
        return annotationDocument;
    }
}

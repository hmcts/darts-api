package uk.gov.hmcts.darts.annotation.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.stream.Collectors;

@Component
public class AnnotationMapper {

    AnnotationDocumentMapper annotationDocumentMapper;

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
                .map(AnnotationDocumentMapper::map)
                .collect(Collectors.toList()));


        return annotation;
    }
}

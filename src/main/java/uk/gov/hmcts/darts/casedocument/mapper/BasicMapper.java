package uk.gov.hmcts.darts.casedocument.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Component
public class BasicMapper {

    Integer convert(UserAccountEntity userAccountEntity) {
        return userAccountEntity.getId();
    }

    Integer convert(MediaEntity mediaEntity) {
        return mediaEntity.getId();
    }

    Integer convert(CaseDocumentEntity caseDocumentEntity) {
        return caseDocumentEntity.getId();
    }

    Integer convert(AnnotationDocumentEntity annotationDocumentEntity) {
        return annotationDocumentEntity.getId();
    }

    Integer convert(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        return transcriptionDocumentEntity.getId();
    }
}

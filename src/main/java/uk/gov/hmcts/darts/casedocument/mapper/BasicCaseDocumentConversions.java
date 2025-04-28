package uk.gov.hmcts.darts.casedocument.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Component
public class BasicCaseDocumentConversions {

    Integer convert(UserAccountEntity userAccountEntity) {
        return userAccountEntity == null ? null : userAccountEntity.getId();
    }

    Long convert(MediaEntity mediaEntity) {
        return mediaEntity == null ? null : mediaEntity.getId();
    }

    Long convert(CaseDocumentEntity caseDocumentEntity) {
        return caseDocumentEntity == null ? null : caseDocumentEntity.getId();
    }

    Long convert(AnnotationDocumentEntity annotationDocumentEntity) {
        return annotationDocumentEntity == null ? null : annotationDocumentEntity.getId();
    }

    Long convert(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        return transcriptionDocumentEntity == null ? null : transcriptionDocumentEntity.getId();
    }

    Integer convert(TranscriptionWorkflowEntity transcriptionWorkflowEntity) {
        return transcriptionWorkflowEntity == null ? null : transcriptionWorkflowEntity.getId();
    }
}

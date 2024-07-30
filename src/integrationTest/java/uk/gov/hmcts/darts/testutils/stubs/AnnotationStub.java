package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AnnotationStub {
    private final UserAccountStubComposable userAccountStub;
    private final AnnotationStubComposable annotationStubComposable;

    public AnnotationEntity createAndSaveAnnotationEntityWith(UserAccountEntity currentOwner,
                                                              String annotationText) {
        return annotationStubComposable.createAndSaveAnnotationEntityWith(currentOwner, annotationText);
    }

    @Transactional
    public AnnotationEntity createAndSaveAnnotationEntityWith(UserAccountEntity currentOwner,
                                                              String annotationText,
                                                              HearingEntity hearingEntity) {
        return annotationStubComposable.createAndSaveAnnotationEntityWith(currentOwner, annotationText, hearingEntity);
    }

    @Transactional
    public AnnotationDocumentEntity createAndSaveAnnotationDocumentEntityWith(AnnotationEntity annotationEntity,
                                                                              String fileName,
                                                                              String fileType,
                                                                              Integer fileSize,
                                                                              UserAccountEntity uploadedBy,
                                                                              OffsetDateTime uploadedDateTime,
                                                                              String checksum) {
        return
            annotationStubComposable.createAndSaveAnnotationDocumentEntityWith(
                annotationEntity, fileName, fileType, fileSize, uploadedBy,uploadedDateTime, checksum);
    }

    @Transactional
    public AnnotationDocumentEntity createAnnotationDocumentEntity(AnnotationEntity annotationEntity, String fileName, String fileType, Integer fileSize,
                                                                   UserAccountEntity uploadedBy, OffsetDateTime uploadedDateTime, String checksum) {
        return annotationStubComposable.createAnnotationDocumentEntity(annotationEntity, fileName, fileType, fileSize, uploadedBy,uploadedDateTime, checksum);
    }

    @Transactional
    public AnnotationDocumentEntity createAndSaveAnnotationDocumentEntity(AnnotationEntity annotation) {
        return annotationStubComposable.createAndSaveAnnotationDocumentEntity(userAccountStub, annotation);
    }

}
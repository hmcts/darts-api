package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AnnotationStub {

    private final AnnotationRepository annotationRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;

    public AnnotationEntity createAndSaveAnnotationEntityWith(UserAccountEntity currentOwner,
          String annotationText) {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setCurrentOwner(currentOwner);
        annotationEntity.setText(annotationText);
        return annotationRepository.save(annotationEntity);
    }

    public AnnotationDocumentEntity createAndSaveAnnotationDocumentEntityWith(AnnotationEntity annotationEntity,
          String fileName,
          String fileType,
          Integer fileSize,
          UserAccountEntity uploadedBy,
          OffsetDateTime uploadedDateTime,
          String checksum) {
        AnnotationDocumentEntity annotationDocument = new AnnotationDocumentEntity();
        annotationDocument.setAnnotation(annotationEntity);
        annotationDocument.setFileName(fileName);
        annotationDocument.setFileType(fileType);
        annotationDocument.setFileSize(fileSize);
        annotationDocument.setUploadedBy(uploadedBy);
        annotationDocument.setUploadedDateTime(uploadedDateTime);
        annotationDocument.setChecksum(checksum);
        annotationDocumentRepository.save(annotationDocument);
        return annotationDocument;
    }
}

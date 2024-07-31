package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AnnotationStubComposable {

    private final AnnotationRepository annotationRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;

    public AnnotationEntity createAndSaveAnnotationEntityWith(UserAccountEntity currentOwner,
                                                              String annotationText) {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setCurrentOwner(currentOwner);
        annotationEntity.setText(annotationText);
        return annotationRepository.save(annotationEntity);
    }

    @Transactional
    public AnnotationEntity createAndSaveAnnotationEntityWith(UserAccountEntity currentOwner,
                                                              String annotationText,
                                                              HearingEntity hearingEntity) {
        AnnotationEntity annotationEntity = createAnnotationEntity(currentOwner, annotationText);
        annotationEntity.addHearing(hearingEntity);
        return annotationRepository.save(annotationEntity);
    }

    @Transactional
    public AnnotationDocumentEntity createAndSaveAnnotationDocumentEntity(UserAccountStubComposable userAccountStubComposable, AnnotationEntity annotation) {

        // this is so that annotation becomes managed by JPA and its hearing list is loaded
        var managedAnnotation = annotationRepository.findById(annotation.getId()).get();
        managedAnnotation.getHearingList().size();

        UserAccountEntity testUser = userAccountStubComposable.getIntegrationTestUserAccountEntity();

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "123";

        return createAndSaveAnnotationDocumentEntityWith(managedAnnotation, fileName, fileType, fileSize, testUser, uploadedDateTime, checksum);
    }

    @Transactional
    public AnnotationDocumentEntity createAndSaveAnnotationDocumentEntityWith(AnnotationEntity annotationEntity,
                                                                              String fileName,
                                                                              String fileType,
                                                                              Integer fileSize,
                                                                              UserAccountEntity uploadedBy,
                                                                              OffsetDateTime uploadedDateTime,
                                                                              String checksum) {
        AnnotationDocumentEntity annotationDocument = createAnnotationDocumentEntity(annotationEntity, fileName, fileType, fileSize,
                                                                                     uploadedBy, uploadedDateTime, checksum);
        annotationDocument.setAnnotation(annotationRepository.getReferenceById(annotationEntity.getId()));
        annotationDocument = annotationDocumentRepository.saveAndFlush(annotationDocument);
        return annotationDocument;
    }

    public static AnnotationEntity createAnnotationEntity(UserAccountEntity currentOwner, String annotationText) {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setCurrentOwner(currentOwner);
        annotationEntity.setText(annotationText);
        annotationEntity.setTimestamp(OffsetDateTime.now());
        annotationEntity.setCreatedBy(currentOwner);
        return annotationEntity;
    }

    @Transactional
    public AnnotationDocumentEntity createAnnotationDocumentEntity(AnnotationEntity annotationEntity, String fileName, String fileType, Integer fileSize,
                                                                   UserAccountEntity uploadedBy, OffsetDateTime uploadedDateTime, String checksum) {
        AnnotationDocumentEntity annotationDocument = new AnnotationDocumentEntity();
        annotationDocument.setAnnotation(annotationEntity);
        annotationDocument.setFileName(fileName);
        annotationDocument.setFileType(fileType);
        annotationDocument.setFileSize(fileSize);
        annotationDocument.setUploadedBy(uploadedBy);
        annotationDocument.setUploadedDateTime(uploadedDateTime);
        annotationDocument.setChecksum(checksum);
        annotationDocument.setLastModifiedBy(uploadedBy);

        return annotationDocument;
    }
}
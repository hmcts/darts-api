package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestAnnotationDocumentEntity extends AnnotationDocumentEntity implements DbInsertable<AnnotationDocumentEntity> {

    @lombok.Builder
    public TestAnnotationDocumentEntity(Long id, String fileName, String fileType, Integer fileSize,
                                        UserAccountEntity uploadedBy, OffsetDateTime uploadedDateTime,
                                        String checksum, boolean isDeleted, UserAccountEntity deletedBy,
                                        OffsetDateTime deletedTs, String contentObjectId, String clipId,
                                        boolean isHidden, OffsetDateTime retainUntilTs, RetentionConfidenceScoreEnum retConfScore,
                                        String retConfReason, AnnotationEntity annotation,
                                        OffsetDateTime lastModifiedTimestamp, Integer lastModifiedById) {
        super();
        setId(id);
        setFileName(fileName);
        setFileType(fileType);
        setFileSize(fileSize);
        setUploadedBy(uploadedBy);
        setUploadedDateTime(uploadedDateTime);
        setChecksum(checksum);
        setDeleted(isDeleted);
        setDeletedBy(deletedBy);
        setDeletedTs(deletedTs);
        setContentObjectId(contentObjectId);
        setClipId(clipId);
        setHidden(isHidden);
        setRetainUntilTs(retainUntilTs);
        setRetConfScore(retConfScore);
        setRetConfReason(retConfReason);
        setAnnotation(annotation);
        setLastModifiedTimestamp(lastModifiedTimestamp);
        setLastModifiedById(lastModifiedById);
    }

    @Override
    public AnnotationDocumentEntity getEntity() {
        try {
            AnnotationDocumentEntity annotationEntity = new AnnotationDocumentEntity();
            BeanUtils.copyProperties(annotationEntity, this);
            return annotationEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestAnnotationDocumentEntityRetrieve implements
        BuilderHolder<TestAnnotationDocumentEntity, TestAnnotationDocumentEntity.TestAnnotationDocumentEntityBuilder> {

        private final TestAnnotationDocumentEntity.TestAnnotationDocumentEntityBuilder builder = TestAnnotationDocumentEntity.builder();

        @Override
        public TestAnnotationDocumentEntity build() {
            return builder.build();
        }

        @Override
        public TestAnnotationDocumentEntity.TestAnnotationDocumentEntityBuilder getBuilder() {
            return builder;
        }
    }
}
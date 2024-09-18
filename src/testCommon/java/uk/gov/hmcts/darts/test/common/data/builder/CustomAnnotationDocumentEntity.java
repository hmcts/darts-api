package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CustomAnnotationDocumentEntity extends AnnotationDocumentEntity implements DbInsertable<AnnotationDocumentEntity> {

    @lombok.Builder
    public CustomAnnotationDocumentEntity(Integer id, String fileName, String fileType, Integer fileSize,
                                          UserAccountEntity uploadedBy, OffsetDateTime uploadedDateTime,
                                          String checksum, boolean isDeleted, UserAccountEntity deletedBy,
                                          OffsetDateTime deletedTs, String contentObjectId, String clipId,
                                          boolean isHidden, OffsetDateTime retainUntilTs, Integer retConfScore,
                                          String retConfReason, AnnotationEntity annotation,
                                          OffsetDateTime lastModifiedTimestamp, UserAccountEntity lastModifiedBy) {
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
        setLastModifiedBy(lastModifiedBy);
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

    public static class CustomAnnotationDocumentEntityRetrieve implements
        BuilderHolder<CustomAnnotationDocumentEntity, CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityBuilder> {
        public CustomAnnotationDocumentEntityRetrieve() {
        }

        private CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityBuilder builder = CustomAnnotationDocumentEntity.builder();

        @Override
        public CustomAnnotationDocumentEntity build() {
            return builder.build();
        }

        @Override
        public CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityBuilder getBuilder() {
            return builder;
        }
    }
}
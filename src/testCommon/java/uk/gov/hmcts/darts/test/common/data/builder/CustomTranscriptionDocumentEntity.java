package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.Persistable;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CustomTranscriptionDocumentEntity extends TranscriptionDocumentEntity implements DbInsertable<TranscriptionDocumentEntity> {
    @lombok.Builder
    public CustomTranscriptionDocumentEntity(Integer id, TranscriptionEntity transcription, String clipId, String fileName, String fileType, Integer fileSize, UserAccountEntity uploadedBy, OffsetDateTime uploadedDateTime, List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities, String checksum, boolean isDeleted, UserAccountEntity deletedBy, OffsetDateTime deletedTs, String contentObjectId, boolean isHidden, OffsetDateTime retainUntilTs, Integer retConfScore, String retConfReason, List<ObjectAdminActionEntity> adminActions, OffsetDateTime createdDateTime, UserAccountEntity createdBy, OffsetDateTime lastModifiedDateTime, UserAccountEntity lastModifiedBy) {
        setId(id);
        setTranscription(transcription);
        setClipId(clipId);
        setFileName(fileName);
        setFileType(fileType);
        setFileSize(fileSize);
        setUploadedBy(uploadedBy);
        setUploadedDateTime(uploadedDateTime);
        setExternalObjectDirectoryEntities(externalObjectDirectoryEntities);
        setChecksum(checksum);
        setDeleted(isDeleted);
        setDeletedBy(deletedBy);
        setDeletedTs(deletedTs);
        setContentObjectId(contentObjectId);
        setHidden(isHidden);
        setRetainUntilTs(retainUntilTs);
        setRetConfScore(retConfScore);
        setRetConfReason(retConfReason);
        setAdminActions(adminActions);
        setLastModifiedBy(createdBy);
        setLastModifiedTimestamp(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public TranscriptionDocumentEntity getDbInsertable() {
        try {
            TranscriptionDocumentEntity transcriptionEntity = new TranscriptionDocumentEntity();
            BeanUtils.copyProperties(transcriptionEntity, this);
            return transcriptionEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TranscriptionDocumentEntityBuilderRetrieve implements BuilderHolder<CustomTranscriptionDocumentEntity, CustomTranscriptionDocumentEntity.CustomTranscriptionDocumentEntityBuilder> {

        private CustomTranscriptionDocumentEntity.CustomTranscriptionDocumentEntityBuilder builder = CustomTranscriptionDocumentEntity.builder();

        @Override
        public CustomTranscriptionDocumentEntity build() {
           return builder.build();
        }

        @Override
        public CustomTranscriptionDocumentEntity.CustomTranscriptionDocumentEntityBuilder getBuilder() {
            return builder;
        }
    }
}
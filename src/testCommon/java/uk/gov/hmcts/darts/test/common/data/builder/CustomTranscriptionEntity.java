package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CustomTranscriptionEntity extends TranscriptionEntity implements DbInsertable<TranscriptionEntity> {
    @lombok.Builder
    public CustomTranscriptionEntity(Integer id, List<CourtCaseEntity> courtCases,
                                     TranscriptionTypeEntity transcriptionType,
                                     CourtroomEntity courtroom,
                                     TranscriptionUrgencyEntity transcriptionUrgency,
                                     List<HearingEntity> hearings,
                                     TranscriptionStatusEntity transcriptionStatus,
                                     String legacyObjectId, UserAccountEntity requestedBy,
                                     LocalDate hearingDate, OffsetDateTime startTime,
                                     OffsetDateTime endTime, String legacyVersionLabel,
                                     Boolean isManualTranscription, Boolean isCurrent,
                                     Boolean hideRequestFromRequestor, boolean isDeleted,
                                     UserAccountEntity deletedBy, OffsetDateTime deletedTimestamp,
                                     String chronicleId, String antecedentId,
                                     List<TranscriptionCommentEntity> transcriptionCommentEntities,
                                     List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities,
                                     List<TranscriptionDocumentEntity> transcriptionDocumentEntities,
                                     String transcriptionObjectName, OffsetDateTime createdDateTime,
                                     UserAccountEntity createdBy,
                                     OffsetDateTime lastModifiedDateTime,
                                     UserAccountEntity lastModifiedBy) {
        setId(id);
        setCourtCases(courtCases);
        setTranscriptionType(transcriptionType);
        setCourtroom(courtroom);
        setTranscriptionUrgency(transcriptionUrgency);
        setHearings(hearings);
        setTranscriptionStatus(transcriptionStatus);
        setLegacyObjectId(legacyObjectId);
        setRequestedBy(requestedBy);
        setHearingDate(hearingDate);
        setStartTime(startTime);
        setEndTime(endTime);
        setLegacyVersionLabel(legacyVersionLabel);
        setIsManualTranscription(isManualTranscription);
        setIsCurrent(isCurrent);
        setHideRequestFromRequestor(hideRequestFromRequestor);
        setDeleted(isDeleted);
        setDeletedBy(deletedBy);
        setDeletedTimestamp(deletedTimestamp);
        setChronicleId(chronicleId);
        setAntecedentId(antecedentId);
        setTranscriptionCommentEntities(transcriptionCommentEntities);
        setTranscriptionWorkflowEntities(transcriptionWorkflowEntities);
        setTranscriptionDocumentEntities(transcriptionDocumentEntities);
        setTranscriptionObjectName(transcriptionObjectName);
        setCreatedDateTime(createdDateTime);
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public TranscriptionEntity getDbInsertable() {
        try {
            TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
            BeanUtils.copyProperties(transcriptionEntity, this);
            return transcriptionEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class CustomTranscriptionEntityBuilderRetrieve implements BuilderHolder<CustomTranscriptionEntity, CustomTranscriptionEntityBuilder> {

        private final CustomTranscriptionEntity.CustomTranscriptionEntityBuilder builder = CustomTranscriptionEntity.builder();

        @Override
        public CustomTranscriptionEntity build() {
            return builder.build();
        }

        @Override
        public CustomTranscriptionEntity.CustomTranscriptionEntityBuilder getBuilder() {
            return builder;
        }
    }
}
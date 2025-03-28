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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestTranscriptionEntity extends TranscriptionEntity implements DbInsertable<TranscriptionEntity> {
    @lombok.Builder
    public TestTranscriptionEntity(Integer id, List<CourtCaseEntity> courtCases,
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
                                   Integer createdById,
                                   OffsetDateTime lastModifiedDateTime,
                                   Integer lastModifiedById) {
        setId(id);
        setCourtCases(courtCases);
        setTranscriptionType(transcriptionType);
        setCourtroom(courtroom);
        setTranscriptionUrgency(transcriptionUrgency);
        setHearings(hearings != null ? hearings : new ArrayList<>());
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
        setTranscriptionCommentEntities(transcriptionCommentEntities != null ? transcriptionCommentEntities : new ArrayList<>());
        setTranscriptionWorkflowEntities(transcriptionWorkflowEntities != null ? transcriptionWorkflowEntities : new ArrayList<>());
        setTranscriptionDocumentEntities(transcriptionDocumentEntities != null ? transcriptionDocumentEntities : new ArrayList<>());
        setTranscriptionObjectName(transcriptionObjectName);
        setCreatedDateTime(createdDateTime);
        setCreatedById(Optional.ofNullable(createdById).orElse(0));
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(Optional.ofNullable(lastModifiedById).orElse(0));
    }

    @Override
    public TranscriptionEntity getEntity() {
        try {
            TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
            BeanUtils.copyProperties(transcriptionEntity, this);
            //Ensures setting of createdBy does not override setting getCreatedById.
            transcriptionEntity.setCreatedById(this.getCreatedById());
            return transcriptionEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestTranscriptionEntityBuilderRetrieve implements BuilderHolder<TestTranscriptionEntity, TestTranscriptionEntityBuilder> {

        private final TestTranscriptionEntity.TestTranscriptionEntityBuilder builder = TestTranscriptionEntity.builder();

        @Override
        public TestTranscriptionEntity build() {
            return builder.build();
        }

        @Override
        public TestTranscriptionEntity.TestTranscriptionEntityBuilder getBuilder() {
            return builder;
        }
    }
}
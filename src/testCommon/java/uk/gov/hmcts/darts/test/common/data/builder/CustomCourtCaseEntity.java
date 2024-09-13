package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CustomCourtCaseEntity extends CourtCaseEntity implements DbInsertable<CourtCaseEntity> {

    @lombok.Builder
    public CustomCourtCaseEntity(
        Integer id,
        EventHandlerEntity reportingRestrictions,
        String legacyCaseObjectId,
        String caseNumber,
        CourthouseEntity courthouse,
        Boolean closed,
        Boolean interpreterUsed,
        OffsetDateTime caseClosedTimestamp,
        Boolean retentionUpdated,
        Integer retentionRetries,
        List<DefendantEntity> defendantList,
        List<ProsecutorEntity> prosecutorList,
        List<DefenceEntity> defenceList,
        Boolean deleted,
        UserAccountEntity deletedBy,
        OffsetDateTime deletedTimestamp,
        List<HearingEntity> hearings,
        List<CaseRetentionEntity> caseRetentionEntities,
        List<JudgeEntity> judges,
        List<MediaLinkedCaseEntity> mediaLinkedCaseList,
        Boolean dataAnonymised,
        Integer dataAnonymisedBy,
        OffsetDateTime dataAnonymisedTs,
        String caseType,
        Integer uploadPriority,
        RetentionConfidenceScoreEnum retConfScore,
        RetentionConfidenceReasonEnum retConfReason,
        OffsetDateTime retConfUpdatedTs,
        OffsetDateTime createdDateTime,
        UserAccountEntity createdBy,
        OffsetDateTime lastModifiedDateTime,
        UserAccountEntity lastModifiedBy
    ) {
        setId(id);
        setReportingRestrictions(reportingRestrictions);
        setLegacyCaseObjectId(legacyCaseObjectId);
        setCaseNumber(caseNumber);
        setCourthouse(courthouse);
        setClosed(closed);
        setInterpreterUsed(interpreterUsed);
        setCaseClosedTimestamp(caseClosedTimestamp);
        setRetentionUpdated(retentionUpdated);
        setRetentionRetries(retentionRetries);
        setDefendantList(defendantList);
        setProsecutorList(prosecutorList);
        setDefenceList(defenceList);
        setDeleted(deleted);
        setDeletedBy(deletedBy);
        setDeletedTimestamp(deletedTimestamp);
        setHearings(hearings);
        setCaseRetentionEntities(caseRetentionEntities);
        setJudges(judges);
        setMediaLinkedCaseList(mediaLinkedCaseList);
        setDataAnonymised(dataAnonymised);
        setDataAnonymisedBy(dataAnonymisedBy);
        setDataAnonymisedTs(dataAnonymisedTs);
        setCaseType(caseType);
        setUploadPriority(uploadPriority);
        setRetConfScore(retConfScore);
        setRetConfReason(retConfReason);
        setRetConfUpdatedTs(retConfUpdatedTs);
        setCreatedDateTime(createdDateTime);
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public CourtCaseEntity getDbInsertable() {
        try {
            CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
            BeanUtils.copyProperties(courtCaseEntity, this);
            return courtCaseEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class CustomTranscriptionEntityBuilderRetrieve implements BuilderHolder<CustomCourtCaseEntity, CustomCourtCaseEntity.CustomCourtCaseEntityBuilder> {

        private final CustomCourtCaseEntity.CustomCourtCaseEntityBuilder builder = CustomCourtCaseEntity.builder();

        @Override
        public CustomCourtCaseEntity build() {
           return builder.build();
        }

        @Override
        public CustomCourtCaseEntity.CustomCourtCaseEntityBuilder getBuilder() {
            return builder;
        }
    }
}
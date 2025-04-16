package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestCourtCaseEntity extends CourtCaseEntity implements DbInsertable<CourtCaseEntity> {

    @lombok.Builder
    public TestCourtCaseEntity(
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
        Integer createdById,
        OffsetDateTime lastModifiedDateTime,
        Integer lastModifiedById
    ) {
        super();
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
        setDefendantList(defendantList != null ? defendantList : new ArrayList<>());
        setProsecutorList(prosecutorList != null ? prosecutorList : new ArrayList<>());
        setDefenceList(defenceList != null ? defenceList : new ArrayList<>());
        setDeleted(deleted);
        setDeletedBy(deletedBy);
        setDeletedTimestamp(deletedTimestamp);
        setHearings(hearings != null ? hearings : new ArrayList<>());
        setCaseRetentionEntities(caseRetentionEntities != null ? caseRetentionEntities : new ArrayList<>());
        setJudges(judges != null ? judges : new ArrayList<>());
        setMediaLinkedCaseList(mediaLinkedCaseList != null ? mediaLinkedCaseList : new ArrayList<>());
        setDataAnonymised(dataAnonymised);
        setDataAnonymisedBy(dataAnonymisedBy);
        setDataAnonymisedTs(dataAnonymisedTs);
        setCaseType(caseType);
        setUploadPriority(uploadPriority);
        setRetConfScore(retConfScore);
        setRetConfReason(retConfReason);
        setRetConfUpdatedTs(retConfUpdatedTs);
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(lastModifiedById);
    }

    @Override
    public CourtCaseEntity getEntity() {
        try {
            CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
            BeanUtils.copyProperties(courtCaseEntity, this);
            return courtCaseEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestCourtCaseBuilderRetrieve
        implements BuilderHolder<TestCourtCaseEntity, TestCourtCaseEntity.TestCourtCaseEntityBuilder> {

        private final TestCourtCaseEntity.TestCourtCaseEntityBuilder builder = TestCourtCaseEntity.builder();

        @Override
        public TestCourtCaseEntity build() {
            return builder.build();
        }

        @Override
        public TestCourtCaseEntity.TestCourtCaseEntityBuilder getBuilder() {
            return builder;
        }
    }
}
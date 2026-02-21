package uk.gov.hmcts.darts.retention.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.retention.service.impl.ApplyRetentionProcessorImpl;
import uk.gov.hmcts.darts.retention.util.RetentionConfidenceCategoryUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyRetentionProcessorIntTest extends IntegrationBase {

    private static final OffsetDateTime CASE_CLOSED_TIME = OffsetDateTime.now().minusDays(8);
    private static final String RETAIN_UNTIL = "2030-01-01T12:00Z";

    @Autowired
    private CaseRetentionRepository caseRetentionRepository;
    @Autowired
    private ApplyRetentionProcessorImpl applyRetentionProcessor;
    @Autowired
    private RetentionConfidenceCategoryUtil retentionConfidenceCategoryUtil;
    private CourtCaseEntity courtCase;

    @BeforeEach
    void setUp() {
        courtCase = dartsDatabase.createCase(
            "a courthouse",
            "a case"
        );
        courtCase.setClosed(true);
        courtCase.setCaseClosedTimestamp(CASE_CLOSED_TIME);
        dartsDatabase.save(courtCase);

        OffsetDateTime retainUntilDate = OffsetDateTime.parse(RETAIN_UNTIL);

        CaseRetentionEntity caseRetentionEntity = dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.PENDING, retainUntilDate, false);
        caseRetentionEntity.setCreatedDateTime(CASE_CLOSED_TIME);
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

        retentionConfidenceCategoryUtil.createAndSaveRetentionConfidenceCategoryMappings();
    }

    @Test
    void processApplyRetention_ShouldSucceed() {
        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.getFirst();
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntity.getCurrentState());
        caseRetentionEntity.setConfidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED.getId());

        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

        applyRetentionProcessor.processApplyRetention(1000);

        transactionalUtil.executeInTransaction(() -> {
            List<CaseRetentionEntity> caseRetentionEntitiesPostUpdate = caseRetentionRepository.findAllByCourtCase(courtCase);
            CaseRetentionEntity caseRetentionEntityPostUpdate = caseRetentionEntitiesPostUpdate.getFirst();

            assertEquals(1, caseRetentionEntitiesPostUpdate.size());
            assertTrue(caseRetentionEntityPostUpdate.getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
            assertEquals(caseRetentionEntityPostUpdate.getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS),
                         OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
            assertEquals(CaseRetentionStatus.COMPLETE.name(), caseRetentionEntityPostUpdate.getCurrentState());

            CourtCaseEntity courtCaseEntity = caseRetentionEntityPostUpdate.getCourtCase();
            assertTrue(courtCaseEntity.isRetentionUpdated());
            assertEquals(0, courtCaseEntity.getRetentionRetries());
            assertEquals(RetentionConfidenceReasonEnum.CASE_CLOSED, courtCaseEntity.getRetConfReason());
            assertEquals(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, courtCaseEntity.getRetConfScore());
            assertNotNull(courtCaseEntity.getRetConfUpdatedTs());

        });
    }

    @Test
    void processApplyRetention_ShouldNotApplyRetention_WhenCloseDateWithRecordInsideCoolOff() {
        OffsetDateTime caseClosedTime = OffsetDateTime.now().minusDays(6);
        courtCase.setCaseClosedTimestamp(caseClosedTime);
        dartsDatabase.save(courtCase);

        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.getFirst();
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntity.getCurrentState());
        applyRetentionProcessor.processApplyRetention(1000);

        transactionalUtil.executeInTransaction(() -> {
            List<CaseRetentionEntity> caseRetentionEntitiesPostUpdate = caseRetentionRepository.findAllByCourtCase(courtCase);
            CaseRetentionEntity caseRetentionEntityPostUpdate = caseRetentionEntitiesPostUpdate.getFirst();

            assertEquals(1, caseRetentionEntitiesPostUpdate.size());
            assertTrue(caseRetentionEntityPostUpdate.getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
            assertEquals(caseRetentionEntityPostUpdate.getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS),
                         OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
            assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntityPostUpdate.getCurrentState());

            assertFalse(caseRetentionEntityPostUpdate.getCourtCase().isRetentionUpdated());
            assertNull(caseRetentionEntityPostUpdate.getCourtCase().getRetentionRetries());
        });
    }

    @Test
    void processApplyRetention_MultipleCaseRetentionsApplyMostRecent() {
        OffsetDateTime retainUntilDate = OffsetDateTime.parse(RETAIN_UNTIL);

        CaseRetentionEntity caseRetentionEntity = dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.PENDING, retainUntilDate, false);
        caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now().minusDays(9));
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntities.getFirst().getCurrentState());
        applyRetentionProcessor.processApplyRetention(1000);

        caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        assertTrue(caseRetentionEntities.getFirst().getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
        assertEquals(caseRetentionEntities.getFirst().getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS),
                     OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
        assertEquals(CaseRetentionStatus.COMPLETE.name(), caseRetentionEntities.getFirst().getCurrentState());

        assertEquals(CaseRetentionStatus.IGNORED.name(), caseRetentionEntities.get(1).getCurrentState());
        assertEquals(caseRetentionEntities.get(1).getCreatedDateTime().truncatedTo(ChronoUnit.DAYS),
                     OffsetDateTime.now().minusDays(9).truncatedTo(ChronoUnit.DAYS));

    }
}

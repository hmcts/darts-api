package uk.gov.hmcts.darts.retention.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.service.impl.ApplyRetentionProcessorImpl;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ApplyRetentionProcessorIntTest extends IntegrationBase {

    @Autowired
    CaseRetentionRepository caseRetentionRepository;
    @Autowired
    ApplyRetentionProcessorImpl applyRetentionProcessor;
    CourtCaseEntity courtCase;

    @BeforeEach
    void setUp() {
        courtCase = dartsDatabase.createCase(
            "a courthouse",
            "a case"
        );
        OffsetDateTime caseClosedTime = OffsetDateTime.now().minusDays(8);
        courtCase.setClosed(true);
        courtCase.setCaseClosedTimestamp(caseClosedTime);
        dartsDatabase.save(courtCase);

        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2030-01-01T12:00Z");

        CaseRetentionEntity caseRetentionEntity = dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.PENDING, retainUntilDate, false);
        caseRetentionEntity.setCreatedDateTime(caseClosedTime);
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

    }

    @Test
    void testCaseRetentionChangeState() {
        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.getFirst();
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntity.getCurrentState());
        applyRetentionProcessor.processApplyRetention(1000);

        caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        caseRetentionEntity = caseRetentionEntities.getFirst();

        assertEquals(1, caseRetentionEntities.size());
        assertTrue(caseRetentionEntity.getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
        assertEquals(caseRetentionEntity.getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS), OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
        assertEquals(CaseRetentionStatus.COMPLETE.name(), caseRetentionEntity.getCurrentState());

        assertTrue(caseRetentionEntity.getCourtCase().isRetentionUpdated());
        assertEquals(0, caseRetentionEntity.getCourtCase().getRetentionRetries());
    }

    @Test
    void testCaseCloseDateWithRecordInsideCoolOff() {
        OffsetDateTime caseClosedTime = OffsetDateTime.now().minusDays(6);
        courtCase.setCaseClosedTimestamp(caseClosedTime);
        dartsDatabase.save(courtCase);

        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.getFirst();
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntity.getCurrentState());
        applyRetentionProcessor.processApplyRetention(1000);

        caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        caseRetentionEntity = caseRetentionEntities.getFirst();

        assertEquals(1, caseRetentionEntities.size());
        assertTrue(caseRetentionEntity.getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
        assertEquals(caseRetentionEntity.getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS), OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntity.getCurrentState());

        assertFalse(caseRetentionEntity.getCourtCase().isRetentionUpdated());
        assertNull(caseRetentionEntity.getCourtCase().getRetentionRetries());
    }

    @Test
    void givenMultipleCaseRetentionsApplyMostRecent() {
        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2030-01-01T12:00Z");

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

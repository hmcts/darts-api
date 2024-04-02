package uk.gov.hmcts.darts.retention.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.service.impl.ApplyRetentionProcessorImpl;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ApplyRetentionProcessorIntTest extends IntegrationBase {

    @Autowired
    CaseRetentionRepository caseRetentionRepository;

    @Autowired
    CurrentTimeHelper currentTimeHelper;
    @Autowired
    ApplyRetentionProcessorImpl applyRetentionProcessor;
    DartsDatabaseStub dartsDatabaseStub;
    CourtCaseEntity courtCase;

    @BeforeEach
    void setUp() {
        courtCase = dartsDatabase.createCase(
                "a courthouse",
                "a case"
        );
        courtCase.setClosed(true);
        dartsDatabase.save(courtCase);

        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2030-01-01T12:00Z");

        CaseRetentionEntity caseRetentionEntity = dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.PENDING, retainUntilDate, false);
        caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now().minusDays(8));
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

    }

    @Test
    void testCaseRetentionChangeState() {
        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntities.get(0).getCurrentState());
        applyRetentionProcessor.processApplyRetention();

        caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);

        assertEquals(1, caseRetentionEntities.size());
        assertTrue(caseRetentionEntities.get(0).getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
        assertEquals(caseRetentionEntities.get(0).getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS), OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
        assertEquals(CaseRetentionStatus.COMPLETE.name(), caseRetentionEntities.get(0).getCurrentState());

        assertEquals(true, caseRetentionEntities.get(0).getCourtCase().getIsRetentionUpdated());
        assertEquals(0, caseRetentionEntities.get(0).getCourtCase().getRetentionRetries());
    }

    @Test
    void testCaseRetentionChangeStateWithRecordInsideCoolOff() {
        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2030-01-01T12:00Z");

        CaseRetentionEntity caseRetentionEntity = dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.PENDING, retainUntilDate, false);
        caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now().minusDays(6));
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntities.get(0).getCurrentState());
        applyRetentionProcessor.processApplyRetention();

        caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        caseRetentionEntities.sort(Comparator.comparing(CaseRetentionEntity::getCreatedDateTime));
        assertTrue(caseRetentionEntities.get(0).getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
        assertEquals(caseRetentionEntities.get(0).getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS), OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
        assertEquals(CaseRetentionStatus.COMPLETE.name(), caseRetentionEntities.get(0).getCurrentState());

        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntities.get(1).getCurrentState());
    }

    @Test
    void givenMultipleCaseRetentionsApplyMostRecent() {
        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2030-01-01T12:00Z");

        CaseRetentionEntity caseRetentionEntity = dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.PENDING, retainUntilDate, false);
        caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now().minusDays(9));
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

        List<CaseRetentionEntity> caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        assertEquals(CaseRetentionStatus.PENDING.name(), caseRetentionEntities.get(0).getCurrentState());
        applyRetentionProcessor.processApplyRetention();

        caseRetentionEntities = caseRetentionRepository.findAllByCourtCase(courtCase);
        assertTrue(caseRetentionEntities.get(0).getCreatedDateTime().isBefore(OffsetDateTime.now().minusDays(7)));
        assertEquals(caseRetentionEntities.get(0).getRetainUntilAppliedOn().truncatedTo(ChronoUnit.DAYS), OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
        assertEquals(CaseRetentionStatus.COMPLETE.name(), caseRetentionEntities.get(0).getCurrentState());

        assertEquals(CaseRetentionStatus.IGNORED.name(), caseRetentionEntities.get(1).getCurrentState());
        assertEquals(caseRetentionEntities.get(1).getCreatedDateTime().truncatedTo(ChronoUnit.DAYS),
                     OffsetDateTime.now().minusDays(9).truncatedTo(ChronoUnit.DAYS));

    }
}

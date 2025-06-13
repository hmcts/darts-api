package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertTrue;

class GenerateCaseDocumentProcessorIntTest extends IntegrationBase {

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private GenerateCaseDocumentProcessor generateCaseDocumentProcessor;

    @Autowired
    private CourtCaseStub caseStub;

    @BeforeEach
    void setupData() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void processGenerateCaseDocument_GeneratesDocument() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        var courtCaseWithCaseDocument = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });
        dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseWithCaseDocument);

        caseStub.createAndSaveCourtCase(courtCase -> courtCase.setClosed(false));

        var matchingCase1 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
            courtCase.setRetentionUpdated(false);
            courtCase.setRetentionRetries(0);
        });
        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            matchingCase1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(28), false);
        dartsDatabase.save(caseRetentionObject1);

        var matchingCase2 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
            courtCase.setRetentionUpdated(false);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            matchingCase2, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(29), false);
        dartsDatabase.save(caseRetentionObject2);

        var caseClosedButMaxedRetentionRetries = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(30));
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(2);
        });
        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            caseClosedButMaxedRetentionRetries, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject3);

        assertThat(dartsDatabase.getCaseRepository().findAll()).hasSize(6);

        // when
        generateCaseDocumentProcessor.processGenerateCaseDocument(2);

        // then
        transactionalUtil.executeInTransaction(() -> {
            CourtCaseEntity courtCaseEntity1 = dartsDatabase.getCaseRepository().findById(matchingCase1.getId()).get();
            assertTrue("", courtCaseEntity1.isRetentionUpdated());
            assertEquals(0, courtCaseEntity1.getRetentionRetries());
            List<CaseDocumentEntity> caseDocumentEntities1 = courtCaseEntity1.getCaseDocumentEntities();
            assertEquals(1, caseDocumentEntities1.size());

            CourtCaseEntity courtCaseEntity2 = dartsDatabase.getCaseRepository().findById(matchingCase2.getId()).get();
            assertTrue("", courtCaseEntity2.isRetentionUpdated());
            assertEquals(0, courtCaseEntity2.getRetentionRetries());
            List<CaseDocumentEntity> caseDocumentEntities2 = courtCaseEntity2.getCaseDocumentEntities();
            assertEquals(1, caseDocumentEntities2.size());

            CourtCaseEntity courtCaseEntity3 = dartsDatabase.getCaseRepository().findById(caseClosedButMaxedRetentionRetries.getId()).get();
            assertTrue("", courtCaseEntity3.isRetentionUpdated());
            assertEquals(2, courtCaseEntity3.getRetentionRetries());
            List<CaseDocumentEntity> caseDocumentEntities3 = courtCaseEntity3.getCaseDocumentEntities();
            assertEquals(0, caseDocumentEntities3.size());
        });
    }

    @Test
    void processGenerateCaseDocument_NoDocumentGenerated_WithClosedDateTooRecent() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        var courtCaseWithCaseDocument = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });
        dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseWithCaseDocument);

        caseStub.createAndSaveCourtCase(courtCase -> courtCase.setClosed(false));

        var recentlyClosedCase = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now());
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            recentlyClosedCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now(), false);
        dartsDatabase.save(caseRetentionObject1);

        var oldClosedCaseWithPendingRetention = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            oldClosedCaseWithPendingRetention, CaseRetentionStatus.PENDING, OffsetDateTime.now().plusDays(29), false);
        dartsDatabase.save(caseRetentionObject2);

        assertThat(dartsDatabase.getCaseRepository().findAll()).hasSize(5);

        // when
        generateCaseDocumentProcessor.processGenerateCaseDocument(2);

        // then
        transactionalUtil.executeInTransaction(() -> {
            CourtCaseEntity courtCaseEntity1 = dartsDatabase.getCaseRepository().findById(recentlyClosedCase.getId()).get();
            List<CaseDocumentEntity> caseDocumentEntities1 = courtCaseEntity1.getCaseDocumentEntities();
            assertEquals(0, caseDocumentEntities1.size());

            CourtCaseEntity courtCaseEntity2 = dartsDatabase.getCaseRepository().findById(oldClosedCaseWithPendingRetention.getId()).get();
            List<CaseDocumentEntity> caseDocumentEntities2 = courtCaseEntity2.getCaseDocumentEntities();
            assertEquals(0, caseDocumentEntities2.size());
        });
    }
}

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
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertTrue;

class GenerateCaseDocumentProcessorIntTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER_1 = "CASE1";
    private static final String SOME_CASE_NUMBER_2 = "CASE2";

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

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
        CourtCaseEntity courtCaseEntity1 = dartsDatabase.getCaseRepository().findById(matchingCase1.getId()).get();
        assertTrue("", courtCaseEntity1.isRetentionUpdated());
        assertEquals(0, courtCaseEntity1.getRetentionRetries());
        List<CaseDocumentEntity> caseDocumentEntities1 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity1);
        assertEquals(1, caseDocumentEntities1.size());

        CourtCaseEntity courtCaseEntity2 = dartsDatabase.getCaseRepository().findById(matchingCase2.getId()).get();
        assertTrue("", courtCaseEntity2.isRetentionUpdated());
        assertEquals(0, courtCaseEntity2.getRetentionRetries());
        List<CaseDocumentEntity> caseDocumentEntities2 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity2);
        assertEquals(1, caseDocumentEntities2.size());

        CourtCaseEntity courtCaseEntity3 = dartsDatabase.getCaseRepository().findById(caseClosedButMaxedRetentionRetries.getId()).get();
        assertTrue("", courtCaseEntity3.isRetentionUpdated());
        assertEquals(2, courtCaseEntity3.getRetentionRetries());
        List<CaseDocumentEntity> caseDocumentEntities3 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity3);
        assertEquals(0, caseDocumentEntities3.size());
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
        CourtCaseEntity courtCaseEntity1 = dartsDatabase.getCaseRepository().findById(recentlyClosedCase.getId()).get();
        List<CaseDocumentEntity> caseDocumentEntities1 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity1);
        assertEquals(0, caseDocumentEntities1.size());

        CourtCaseEntity courtCaseEntity2 = dartsDatabase.getCaseRepository().findById(oldClosedCaseWithPendingRetention.getId()).get();
        List<CaseDocumentEntity> caseDocumentEntities2 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity2);
        assertEquals(0, caseDocumentEntities2.size());
    }
}
